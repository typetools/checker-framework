package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.afu.scenelib.util.JVMNames;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Impure;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/**
 * This is the primary implementation of {@link
 * org.checkerframework.common.wholeprograminference.WholeProgramInference}. It uses an instance of
 * {@link WholeProgramInferenceStorage} to store annotations and to create output files.
 *
 * <p>This class does not perform inference for an element if the element has explicit annotations.
 * That is, calling an {@code update*} method on an explicitly annotated field, method return, or
 * method parameter has no effect.
 *
 * <p>In addition, whole program inference ignores inferred types in a few scenarios. When
 * discovering a use, WPI ignores an inferred type if:
 *
 * <ol>
 *   <li>The inferred type of an element that should be written into a file is a subtype of the
 *       upper bounds of this element's written type in the source code.
 *   <li>The annotation annotates a {@code null} literal, except when doing inference for the
 *       NullnessChecker. (The rationale for this is that {@code null} is a frequently-used default
 *       value, and it would be undesirable to infer the bottom type if {@code null} were the only
 *       value passed as an argument.)
 * </ol>
 *
 * When outputting a file, WPI ignores an inferred type if:
 *
 * <ol>
 *   <li>The @Target annotation does not permit the annotation to be written at this location.
 *   <li>The @RelevantJavaTypes annotation does not permit the annotation to be written at this
 *       location.
 *   <li>The inferred annotation has the @InvisibleQualifier meta-annotation.
 *   <li>The inferred annotation would be the same annotation applied via defaulting &mdash; that
 *       is, if omitting it has the same effect as writing it.
 * </ol>
 *
 * @param <T> the type used by the storage to store annotations
 * @see WholeProgramInferenceStorage
 */
// TODO: We could add an option to update the type of explicitly annotated elements, but this
// currently is not recommended since the insert-annotations-to-source tool, which adds annotations
// from .jaif files into source code, adds annotations on top of existing annotations. See
// https://github.com/typetools/annotation-tools/issues/105 .
// TODO: Ensure that annotations are inserted deterministically into files. This is important for
// debugging and comparison; otherwise running the whole-program inference on the same set of files
// can yield different results (order of annotations).
public class WholeProgramInferenceImplementation<T> implements WholeProgramInference {

  /** The type factory associated with this. */
  protected final AnnotatedTypeFactory atypeFactory;

  /**
   * Whether to print debugging information when an inference is attempted, but cannot be completed.
   * An inference can be attempted without success for example because the current storage system
   * does not support placing annotation in the location for which an annotation was inferred.
   */
  private final boolean showWpiFailedInferences;

  /** The storage for the inferred annotations. */
  private final WholeProgramInferenceStorage<T> storage;

  /** Whether to ignore assignments where the rhs is null. */
  private final boolean ignoreNullAssignments;

  /** The @{@link Deterministic} annotation. */
  private final AnnotationMirror DETERMINISTIC;

  /** The @{@link SideEffectFree} annotation. */
  private final AnnotationMirror SIDE_EFFECT_FREE;

  /** The @{@link Pure} annotation. */
  private final AnnotationMirror PURE;

  /** The @{@link Impure} annotation. */
  private final AnnotationMirror IMPURE;

  /** The fully-qualified name of the @{@link Deterministic} class. */
  private final String DETERMINISTIC_NAME = "org.checkerframework.dataflow.qual.Deterministic";

  /** The fully-qualified name of the @{@link SideEffectFree} class. */
  private final String SIDE_EFFECT_FREE_NAME = "org.checkerframework.dataflow.qual.SideEffectFree";

  /** The fully-qualified name of the @{@link Pure} class. */
  private final String PURE_NAME = "org.checkerframework.dataflow.qual.Pure";

  /** The fully-qualified name of the @{@link Impure} class. */
  private final String IMPURE_NAME = "org.checkerframework.dataflow.qual.Impure";

  /**
   * Constructs a new {@code WholeProgramInferenceImplementation} that has not yet inferred any
   * annotations.
   *
   * @param atypeFactory the associated type factory
   * @param storage the storage used for inferred annotations and for writing output files
   * @param showWpiFailedInferences whether the {@code -AshowWpiFailedInferences} argument was
   *     passed to the checker, and therefore whether to print debugging messages when inference
   *     fails
   */
  public WholeProgramInferenceImplementation(
      AnnotatedTypeFactory atypeFactory,
      WholeProgramInferenceStorage<T> storage,
      boolean showWpiFailedInferences) {
    this.atypeFactory = atypeFactory;
    this.storage = storage;
    boolean isNullness =
        atypeFactory.getClass().getSimpleName().equals("NullnessAnnotatedTypeFactory");
    this.ignoreNullAssignments = !isNullness;
    this.showWpiFailedInferences = showWpiFailedInferences;
    DETERMINISTIC =
        AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), Deterministic.class);
    SIDE_EFFECT_FREE =
        AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), SideEffectFree.class);
    PURE = AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), Pure.class);
    IMPURE = AnnotationBuilder.fromClass(atypeFactory.getElementUtils(), Impure.class);
  }

  /**
   * Returns the storage for inferred annotations.
   *
   * @return the storage for the inferred annotations
   */
  public WholeProgramInferenceStorage<T> getStorage() {
    return storage;
  }

  @Override
  public void updateFromObjectCreation(
      ObjectCreationNode objectCreationNode,
      ExecutableElement constructorElt,
      CFAbstractStore<?, ?> store) {
    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(constructorElt)) {
      return;
    }

    // Don't infer types for code that can't be annotated anyway.
    if (!storage.hasStorageLocationForMethod(constructorElt)) {
      if (showWpiFailedInferences) {
        printFailedInferenceDebugMessage(
            "WPI could not store information"
                + "about this constructor: "
                + JVMNames.getJVMMethodSignature(constructorElt));
      }
      return;
    }

    List<Node> arguments = objectCreationNode.getArguments();
    updateInferredExecutableParameterTypes(
        constructorElt, arguments, null, objectCreationNode.getTree());
    updateContracts(Analysis.BeforeOrAfter.BEFORE, constructorElt, store);
  }

  @Override
  public void updateFromMethodInvocation(
      MethodInvocationNode methodInvNode,
      ExecutableElement methodElt,
      CFAbstractStore<?, ?> store) {
    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    if (!storage.hasStorageLocationForMethod(methodElt)) {
      return;
    }

    // Don't infer formal parameter types from recursive calls.
    //
    // When performing WPI on a library, if there are no external calls (only recursive calls),
    // then each iteration of WPI would make the formal parameter types more restrictive,
    // leading to an infinite (or very long) loop.
    //
    // Consider
    //   void myMethod(int x) { ... myMethod(x-1) ... }`
    // On one iteration, if x has type IntRange(to=100), the recursive call's argument has type
    // IntRange(to=99).  If that is the only call to `MyMethod`, then the formal parameter type
    // would be updated.  On the next iteration it would be refined again to @IntRange(to=98),
    // and so forth.  A recursive call should never restrict a formal parameter type.
    if (isRecursiveCall(methodInvNode)) {
      return;
    }

    List<Node> arguments = methodInvNode.getArguments();
    Node receiver = methodInvNode.getTarget().getReceiver();
    // Static methods have a "receiver" that is a class name rather than an expression.
    // Do not attempt to use the class name as a receiver expression for inference
    // purposes.
    if (receiver instanceof ClassNameNode) {
      receiver = null;
    }
    updateInferredExecutableParameterTypes(methodElt, arguments, receiver, methodInvNode.getTree());
    updateContracts(Analysis.BeforeOrAfter.BEFORE, methodElt, store);
  }

  /**
   * Returns true if the given call is a recursive call.
   *
   * @param methodInvNode a method invocation
   * @return true if the given call is a recursive call
   */
  private boolean isRecursiveCall(MethodInvocationNode methodInvNode) {
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(methodInvNode.getTreePath());
    if (enclosingMethod == null) {
      return false;
    }
    ExecutableElement methodInvocEle = TreeUtils.elementFromUse(methodInvNode.getTree());
    ExecutableElement methodDeclEle = TreeUtils.elementFromDeclaration(enclosingMethod);
    return methodDeclEle.equals(methodInvocEle);
  }

  /**
   * Updates inferred parameter types based on a call to a method or constructor.
   *
   * @param methodElt the element of the method or constructor being invoked
   * @param arguments the arguments of the invocation
   * @param receiver the receiver node, if there is one; null if there is not
   * @param invocationTree the method or constructor invocation, used to viewpoint adapt any
   *     dependent types when storing newly-inferred annotations
   */
  private void updateInferredExecutableParameterTypes(
      ExecutableElement methodElt,
      List<Node> arguments,
      @Nullable Node receiver,
      ExpressionTree invocationTree) {

    String file = storage.getFileForElement(methodElt);
    // Need to check both that receiver is non-null and that this is not a constructor
    // invocation: despite updateFromObjectCreation always passes null, it's possible
    // for updateFromMethodInvocation to actually be a constructor invocation with a
    // receiver: for example, when calling an inner class's constructor, the receiver
    // can be an instance of the enclosing class. Constructor invocations should never
    // have information inferred about their receivers.
    if (receiver != null
        && atypeFactory.wpiShouldInferTypesForReceivers()
        && !methodElt.getSimpleName().contentEquals("<init>")) {
      AnnotatedTypeMirror receiverArgATM = atypeFactory.getReceiverType(invocationTree);
      AnnotatedExecutableType methodDeclType = atypeFactory.getAnnotatedType(methodElt);
      AnnotatedTypeMirror receiverParamATM = methodDeclType.getReceiverType();
      // update the set of annotations for the receiver type if it is not null.
      if (receiverParamATM != null) {
        atypeFactory.wpiAdjustForUpdateNonField(receiverArgATM);
        T receiverAnnotations =
            storage.getReceiverAnnotations(methodElt, receiverParamATM, atypeFactory);
        if (this.atypeFactory instanceof GenericAnnotatedTypeFactory) {
          ((GenericAnnotatedTypeFactory) this.atypeFactory)
              .getDependentTypesHelper()
              .delocalizeAtCallsite(receiverArgATM, invocationTree, arguments, receiver, methodElt);
        }
        updateAnnotationSet(
            receiverAnnotations, TypeUseLocation.RECEIVER, receiverArgATM, receiverParamATM, file);
      }
    }

    int numArguments = arguments.size();
    for (int i = 0; i < numArguments; i++) {
      Node arg = arguments.get(i);
      Tree argTree = arg.getTree();

      VariableElement ve;
      boolean varargsParam = i >= methodElt.getParameters().size() - 1 && methodElt.isVarArgs();
      if (varargsParam && this.atypeFactory.wpiOutputFormat == OutputFormat.JAIF) {
        // The AFU's org.checkerframework.afu.annotator.Main produces a non-compilable
        // source file when JAIF-based WPI tries to output an annotated varargs parameter,
        // such as when running the test
        // checker/tests/ainfer-testchecker/non-annotated/AnonymousAndInnerClass.java.
        // Until that bug is fixed, do not attempt to infer information about varargs
        // parameters in JAIF mode.
        if (showWpiFailedInferences) {
          printFailedInferenceDebugMessage(
              "Annotations cannot be placed on varargs parameters in -Ainfer=jaifs mode, because"
                  + " the JAIF format does not correctly support it. "
                  + "The signature of the method whose varargs parameter was not annotated is: "
                  + JVMNames.getJVMMethodSignature(methodElt));
        }
        return;
      }
      List<? extends VariableElement> params = methodElt.getParameters();
      if (varargsParam) {
        ve = params.get(params.size() - 1);
      } else {
        ve = params.get(i);
      }
      AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
      AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(argTree);
      if (varargsParam) {
        // Check whether argATM needs to be turned into an array type, so that the type
        // structure matches paramATM.
        boolean expandArgATM = false;
        if (argATM.getKind() == TypeKind.ARRAY) {
          int argATMDepth = AnnotatedTypes.getArrayDepth((AnnotatedArrayType) argATM);
          // This unchecked cast is safe because the declared type of a varargs parameter
          // is guaranteed to be an array of some kind.
          int paramATMDepth = AnnotatedTypes.getArrayDepth((AnnotatedArrayType) paramATM);
          if (paramATMDepth != argATMDepth) {
            assert argATMDepth + 1 == paramATMDepth;
            expandArgATM = true;
          }
        } else {
          expandArgATM = true;
        }
        if (expandArgATM) {
          if (argATM.getKind() == TypeKind.WILDCARD) {
            if (showWpiFailedInferences) {
              printFailedInferenceDebugMessage(
                  "Javac cannot create an array type "
                      + "from a wildcard, so WPI did not attempt to infer a type for an array "
                      + "parameter. "
                      + "The signature of the method whose parameter had inference skipped is: "
                      + JVMNames.getJVMMethodSignature(methodElt));
            }
            return;
          }
          AnnotatedTypeMirror argArray =
              AnnotatedTypeMirror.createType(
                  TypesUtils.createArrayType(argATM.getUnderlyingType(), atypeFactory.types),
                  atypeFactory,
                  false);
          ((AnnotatedArrayType) argArray).setComponentType(argATM);
          argATM = argArray;
        }
      }
      atypeFactory.wpiAdjustForUpdateNonField(argATM);
      // If storage.getParameterAnnotations receives an index that's larger than the size
      // of the parameter list, scenes-backed inference can create duplicate entries
      // for the varargs parameter (it indexes inferred annotations by the parameter number).
      int paramIndex = varargsParam ? methodElt.getParameters().size() : i + 1;
      T paramAnnotations =
          storage.getParameterAnnotations(methodElt, paramIndex, paramATM, ve, atypeFactory);
      if (this.atypeFactory instanceof GenericAnnotatedTypeFactory) {
        ((GenericAnnotatedTypeFactory) this.atypeFactory)
            .getDependentTypesHelper()
            .delocalizeAtCallsite(argATM, invocationTree, arguments, receiver, methodElt);
      }
      updateAnnotationSet(paramAnnotations, TypeUseLocation.PARAMETER, argATM, paramATM, file);
    }
  }

  @Override
  public void updateContracts(
      Analysis.BeforeOrAfter preOrPost, ExecutableElement methodElt, CFAbstractStore<?, ?> store) {
    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    if (store == null) {
      throw new BugInCF(
          "updateContracts(%s, %s, null) for %s",
          preOrPost, methodElt, atypeFactory.getClass().getSimpleName());
    }

    if (!storage.hasStorageLocationForMethod(methodElt)) {
      return;
    }

    // TODO: Probably move some part of this into the AnnotatedTypeFactory.

    // This code handles fields of "this" and method parameters (including the receiver
    // parameter "this"), for now.  In the future, extend it to other expressions.
    TypeElement enclosingClass = (TypeElement) methodElt.getEnclosingElement();
    ThisReference thisReference = new ThisReference(enclosingClass.asType());
    ClassName classNameReceiver = new ClassName(enclosingClass.asType());
    // Fields of "this":
    for (VariableElement fieldElement :
        ElementFilter.fieldsIn(enclosingClass.getEnclosedElements())) {
      if (atypeFactory.wpiOutputFormat == OutputFormat.JAIF
          && enclosingClass.getNestingKind().isNested()) {
        // Don't infer facts about fields of inner classes, because IndexFileWriter
        // places the annotations incorrectly on the class declarations.
        continue;
      }
      if (ElementUtils.isStatic(methodElt) && !ElementUtils.isStatic(fieldElement)) {
        // A static method can't have precondition annotations about instance fields.
        continue;
      }
      FieldAccess fa =
          new FieldAccess(
              (ElementUtils.isStatic(fieldElement) ? classNameReceiver : thisReference),
              fieldElement.asType(),
              fieldElement);
      CFAbstractValue<?> v = store.getFieldValue(fa);
      AnnotatedTypeMirror fieldDeclType = atypeFactory.getAnnotatedType(fieldElement);
      AnnotatedTypeMirror inferredType;
      if (v != null) {
        // This field is in the store.
        inferredType = convertCFAbstractValueToAnnotatedTypeMirror(v, fieldDeclType);
        atypeFactory.wpiAdjustForUpdateNonField(inferredType);
      } else {
        // This field is not in the store. Use the declared type.
        inferredType = fieldDeclType;
      }
      T preOrPostConditionAnnos =
          storage.getPreOrPostconditions(
              preOrPost, methodElt, fa.toString(), fieldDeclType, atypeFactory);
      if (preOrPostConditionAnnos == null) {
        continue;
      }
      String file = storage.getFileForElement(methodElt);
      updateAnnotationSet(
          preOrPostConditionAnnos, TypeUseLocation.FIELD, inferredType, fieldDeclType, file, false);
    }
    // Method parameters (other than the receiver parameter "this"):
    // This loop is 1-indexed to match the syntax used in annotation arguments.
    for (int index = 1; index <= methodElt.getParameters().size(); index++) {
      VariableElement paramElt = methodElt.getParameters().get(index - 1);

      // Do not infer information about non-effectively-final method parameters, to avoid
      // spurious flowexpr.parameter.not.final warnings.
      if (!ElementUtils.isEffectivelyFinal(paramElt)) {
        continue;
      }
      LocalVariable param = new LocalVariable(paramElt);
      CFAbstractValue<?> v = store.getValue(param);
      AnnotatedTypeMirror declType = atypeFactory.getAnnotatedType(paramElt);
      AnnotatedTypeMirror inferredType;
      if (v != null) {
        // This parameter is in the store.
        inferredType = convertCFAbstractValueToAnnotatedTypeMirror(v, declType);
        atypeFactory.wpiAdjustForUpdateNonField(inferredType);
      } else {
        // The parameter is not in the store, so don't attempt to create a postcondition for
        // it, since anything other than its default type would not be verifiable. (Only
        // postconditions are supported for parameters.)
        continue;
      }
      T preOrPostConditionAnnos =
          storage.getPreOrPostconditions(preOrPost, methodElt, "#" + index, declType, atypeFactory);
      if (preOrPostConditionAnnos != null) {
        String file = storage.getFileForElement(methodElt);
        updateAnnotationSet(
            preOrPostConditionAnnos,
            TypeUseLocation.PARAMETER,
            inferredType,
            declType,
            file,
            false);
      }
    }
    // Receiver parameter ("this"):
    if (!ElementUtils.isStatic(methodElt)) { // Static methods do not have a receiver.
      CFAbstractValue<?> v = store.getValue(thisReference);
      if (v != null) {
        // This parameter is in the store.
        AnnotatedTypeMirror declaredType =
            atypeFactory.getAnnotatedType(methodElt).getReceiverType();
        if (declaredType == null) {
          // declaredType is null when the method being analyzed is a constructor (which
          // doesn't have a receiver).
          return;
        }
        AnnotatedTypeMirror inferredType =
            AnnotatedTypeMirror.createType(declaredType.getUnderlyingType(), atypeFactory, false);
        inferredType.replaceAnnotations(v.getAnnotations());
        atypeFactory.wpiAdjustForUpdateNonField(inferredType);
        T preOrPostConditionAnnos =
            storage.getPreOrPostconditions(
                preOrPost, methodElt, "this", declaredType, atypeFactory);
        if (preOrPostConditionAnnos != null) {
          String file = storage.getFileForElement(methodElt);
          updateAnnotationSet(
              preOrPostConditionAnnos,
              TypeUseLocation.PARAMETER,
              inferredType,
              declaredType,
              file,
              false);
        }
      }
    }
  }

  /**
   * Converts a CFAbstractValue to an AnnotatedTypeMirror.
   *
   * @param v a value to convert to an AnnotatedTypeMirror
   * @param fieldType an {@code AnnotatedTypeMirror} with the same underlying type as {@code v} that
   *     is copied, then the copy is updated to use {@code v}'s annotations
   * @return a copy of {@code fieldType} with {@code v}'s annotations
   */
  private AnnotatedTypeMirror convertCFAbstractValueToAnnotatedTypeMirror(
      CFAbstractValue<?> v, AnnotatedTypeMirror fieldType) {
    AnnotatedTypeMirror result = fieldType.deepCopy();
    result.replaceAnnotations(v.getAnnotations());
    return result;
  }

  @Override
  public void updateFromOverride(
      MethodTree methodTree,
      ExecutableElement methodElt,
      AnnotatedExecutableType overriddenMethod) {
    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    String file = storage.getFileForElement(methodElt);

    int numParams = overriddenMethod.getParameterTypes().size();
    for (int i = 0; i < numParams; i++) {
      VariableElement ve = methodElt.getParameters().get(i);
      AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
      AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
      atypeFactory.wpiAdjustForUpdateNonField(argATM);
      T paramAnnotations =
          storage.getParameterAnnotations(methodElt, i + 1, paramATM, ve, atypeFactory);
      updateAnnotationSet(paramAnnotations, TypeUseLocation.PARAMETER, argATM, paramATM, file);
    }

    AnnotatedDeclaredType argADT = overriddenMethod.getReceiverType();
    if (argADT != null) {
      AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(methodTree).getReceiverType();
      if (paramATM != null) {
        T receiver = storage.getReceiverAnnotations(methodElt, paramATM, atypeFactory);
        updateAnnotationSet(receiver, TypeUseLocation.RECEIVER, argADT, paramATM, file);
      }
    }
  }

  @Override
  public void updateFromFormalParameterAssignment(
      LocalVariableNode lhs, Node rhs, VariableElement paramElt) {
    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(lhs.getElement())) {
      return;
    }

    Tree rhsTree = rhs.getTree();
    if (rhsTree == null) {
      // TODO: Handle variable-length list as parameter.
      // An ArrayCreationNode with a null tree is created when the
      // parameter is a variable-length list. We are ignoring it for now.
      // See Issue 682: https://github.com/typetools/checker-framework/issues/682
      if (showWpiFailedInferences) {
        printFailedInferenceDebugMessage(
            "Could not update from formal parameter "
                + "assignment, because an ArrayCreationNode with a null tree is created when "
                + "the parameter is a variable-length list. Parameter: "
                + paramElt);
      }
      return;
    }

    ExecutableElement methodElt = (ExecutableElement) paramElt.getEnclosingElement();

    int index_1based = methodElt.getParameters().indexOf(paramElt) + 1;
    if (index_1based == 0) {
      // When paramElt is the parameter of a lambda contained in another
      // method body, the enclosing element is the outer method body
      // rather than the lambda itself (which has no element). WPI
      // does not support inferring types for lambda parameters, so
      // ignore it.
      if (showWpiFailedInferences) {
        printFailedInferenceDebugMessage(
            "Could not update from formal "
                + "parameter assignment inside a lambda expression, because lambda parameters "
                + "cannot be annotated. Parameter: "
                + paramElt);
      }
      return;
    }

    AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(paramElt);
    AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(rhsTree);
    atypeFactory.wpiAdjustForUpdateNonField(argATM);
    T paramAnnotations =
        storage.getParameterAnnotations(methodElt, index_1based, paramATM, paramElt, atypeFactory);
    String file = storage.getFileForElement(methodElt);
    updateAnnotationSet(paramAnnotations, TypeUseLocation.PARAMETER, argATM, paramATM, file);
  }

  @Override
  public void updateFromFieldAssignment(Node lhs, Node rhs) {

    Element element;
    String fieldName;
    if (lhs instanceof FieldAccessNode) {
      element = ((FieldAccessNode) lhs).getElement();
      fieldName = ((FieldAccessNode) lhs).getFieldName();
    } else if (lhs instanceof LocalVariableNode) {
      element = ((LocalVariableNode) lhs).getElement();
      fieldName = ((LocalVariableNode) lhs).getName();
    } else {
      throw new BugInCF(
          "updateFromFieldAssignment received an unexpected node type: " + lhs.getClass());
    }

    // TODO: For a primitive such as long, this is yielding just @GuardedBy rather than
    // @GuardedBy({}).
    AnnotatedTypeMirror rhsATM = atypeFactory.getAnnotatedType(rhs.getTree());
    atypeFactory.wpiAdjustForUpdateField(lhs.getTree(), element, fieldName, rhsATM);

    updateFieldFromType(lhs.getTree(), element, fieldName, rhsATM);
  }

  @Override
  public void updateFieldFromType(
      Tree lhsTree, Element element, String fieldName, AnnotatedTypeMirror rhsATM) {

    if (ignoreFieldInWPI(element, fieldName)) {
      return;
    }

    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(element)) {
      return;
    }

    String file = storage.getFileForElement(element);

    AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(lhsTree);
    T fieldAnnotations = storage.getFieldAnnotations(element, fieldName, lhsATM, atypeFactory);

    if (fieldAnnotations == null) {
      return;
    }

    updateAnnotationSet(fieldAnnotations, TypeUseLocation.FIELD, rhsATM, lhsATM, file);
  }

  /**
   * Returns true if an assignment to the given field should be ignored by WPI.
   *
   * @param element the field's element
   * @param fieldName the field's name
   * @return true if an assignment to the given field should be ignored by WPI
   */
  protected boolean ignoreFieldInWPI(Element element, String fieldName) {
    // Do not attempt to infer types for fields that do not have valid names. For example,
    // compiler-generated temporary variables will have invalid names. Recording facts about
    // fields with invalid names causes jaif-based WPI to crash when reading the .jaif file,
    // and stub-based WPI to generate unparsable stub files.  See
    // https://github.com/typetools/checker-framework/issues/3442
    if (!SourceVersion.isIdentifier(fieldName)) {
      return true;
    }

    // Don't infer types if the inferred field has a declaration annotation with the
    // @IgnoreInWholeProgramInference meta-annotation.
    if (atypeFactory.getDeclAnnotation(element, IgnoreInWholeProgramInference.class) != null
        || atypeFactory
                .getDeclAnnotationWithMetaAnnotation(element, IgnoreInWholeProgramInference.class)
                .size()
            > 0) {
      return true;
    }

    // Don't infer types for code that isn't presented as source.
    if (!ElementUtils.isElementFromSourceCode(element)) {
      return true;
    }

    return false;
  }

  @Override
  public void updateFromReturn(
      ReturnNode retNode,
      ClassSymbol classSymbol,
      MethodTree methodDeclTree,
      Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods) {
    // Don't infer types for code that isn't presented as source.
    if (methodDeclTree == null
        || !ElementUtils.isElementFromSourceCode(
            TreeUtils.elementFromDeclaration(methodDeclTree))) {
      return;
    }

    // Whole-program inference ignores some locations.  See Issue 682:
    // https://github.com/typetools/checker-framework/issues/682
    if (classSymbol == null) { // TODO: Handle anonymous classes.
      return;
    }

    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(methodDeclTree);
    String file = storage.getFileForElement(methodElt);

    AnnotatedTypeMirror lhsATM = atypeFactory.getAnnotatedType(methodDeclTree).getReturnType();
    // Type of the expression returned
    AnnotatedTypeMirror rhsATM = atypeFactory.getAnnotatedType(retNode.getTree().getExpression());
    atypeFactory.wpiAdjustForUpdateNonField(rhsATM);
    DependentTypesHelper dependentTypesHelper =
        ((GenericAnnotatedTypeFactory) atypeFactory).getDependentTypesHelper();
    dependentTypesHelper.delocalize(rhsATM, methodDeclTree);
    T returnTypeAnnos = storage.getReturnAnnotations(methodElt, lhsATM, atypeFactory);
    updateAnnotationSet(returnTypeAnnos, TypeUseLocation.RETURN, rhsATM, lhsATM, file);

    // Now, update return types of overridden methods based on the implementation we just saw.
    // This inference is similar to the inference procedure for method parameters: both are
    // updated based only on the implementations (in this case) or call-sites (for method
    // parameters) that are available to WPI.
    //
    // An alternative implementation would be to:
    //  * update only the method (not overridden methods)
    //  * when finished, propagate the final result to overridden methods
    //
    for (Map.Entry<AnnotatedDeclaredType, ExecutableElement> pair : overriddenMethods.entrySet()) {
      ExecutableElement overriddenMethodElement = pair.getValue();

      // Don't infer types for code that isn't presented as source.
      if (!ElementUtils.isElementFromSourceCode(overriddenMethodElement)) {
        continue;
      }

      AnnotatedExecutableType overriddenMethod =
          atypeFactory.getAnnotatedType(overriddenMethodElement);
      String superClassFile = storage.getFileForElement(overriddenMethodElement);
      AnnotatedTypeMirror overriddenMethodReturnType = overriddenMethod.getReturnType();
      T storedOverriddenMethodReturnTypeAnnotations =
          storage.getReturnAnnotations(
              overriddenMethodElement, overriddenMethodReturnType, atypeFactory);

      updateAnnotationSet(
          storedOverriddenMethodReturnTypeAnnotations,
          TypeUseLocation.RETURN,
          rhsATM,
          overriddenMethodReturnType,
          superClassFile);
    }
  }

  @Override
  public void addMethodDeclarationAnnotation(ExecutableElement methodElt, AnnotationMirror anno) {
    this.addMethodDeclarationAnnotation(methodElt, anno, false);
  }

  @Override
  public void addMethodDeclarationAnnotation(
      ExecutableElement methodElt, AnnotationMirror anno, boolean lubPurity) {

    // Do not infer types for library code, only for type-checked source code.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    // Special-case handling for purity annotations.
    AnnotationMirror annoToAdd;
    if (!(lubPurity && isPurityAnno(anno))) {
      annoToAdd = anno;
    } else {
      // It's a purity annotation and `lubPurity` is true. Do a "least upper bound" between
      // the current purity annotation inferred for the method and anno. This is necessary
      // to avoid WPI inferring incompatible purity annotations on methods that override
      // methods from their superclass.
      // TODO: this would be unnecessary if purity was implemented as a type system.
      AnnotationMirror currentPurityAnno = getPurityAnnotation(methodElt);
      if (currentPurityAnno == null) {
        annoToAdd = anno;
      } else {
        // Clear the current purity annotation, because at this point a new one is
        // definitely going to be inferred.
        storage.removeMethodDeclarationAnnotation(methodElt, currentPurityAnno);
        annoToAdd = lubPurityAnnotations(anno, currentPurityAnno);
      }
    }

    String file = storage.getFileForElement(methodElt);
    boolean isNewAnnotation = storage.addMethodDeclarationAnnotation(methodElt, annoToAdd);
    if (isNewAnnotation) {
      storage.setFileModified(file);
    }
  }

  /**
   * Computes a "least upper bound" between two purity annotations (an annotation is a purity
   * annotation if and only if {@link #isPurityAnno(AnnotationMirror)} returns true). In the
   * "lattice", Impure is the top, SideEffectFree and Deterministic are siblings below it, and Pure
   * is the bottom, below them. Note that this routine is "fail-safe": Impure is returned if either
   * of the input annotations is not actually a purity annotation.
   *
   * @param anno1 a purity annotation
   * @param anno2 another purity annotation
   * @return the "least upper bound" between anno1 and anno2, as described above
   */
  private AnnotationMirror lubPurityAnnotations(AnnotationMirror anno1, AnnotationMirror anno2) {
    // TODO: is this the best way to do this? Would it be easier to just write a real subtype
    // routine for purity? Do we have code to handle this already somewhere?

    boolean anno1IsDet =
        AnnotationUtils.areSameByName(anno1, PURE_NAME)
            || AnnotationUtils.areSameByName(anno1, DETERMINISTIC_NAME);
    boolean anno1IsSEF =
        AnnotationUtils.areSameByName(anno1, PURE_NAME)
            || AnnotationUtils.areSameByName(anno1, SIDE_EFFECT_FREE_NAME);

    boolean anno2IsDet =
        AnnotationUtils.areSameByName(anno2, PURE_NAME)
            || AnnotationUtils.areSameByName(anno2, DETERMINISTIC_NAME);
    boolean anno2IsSEF =
        AnnotationUtils.areSameByName(anno2, PURE_NAME)
            || AnnotationUtils.areSameByName(anno2, SIDE_EFFECT_FREE_NAME);

    if (anno2IsSEF && anno2IsDet && anno1IsSEF && anno1IsDet) {
      return PURE;
    } else if (anno2IsSEF && anno1IsSEF) {
      return SIDE_EFFECT_FREE;
    } else if (anno2IsDet && anno1IsDet) {
      return DETERMINISTIC;
    } else {
      return IMPURE;
    }
  }

  /**
   * Returns the purity annotation ({@link Pure}, {@link SideEffectFree}, {@link Deterministic}, or
   * {@link Impure}) currently associated with the given executable element in this round of
   * inference, if there is one. Invariant: no more than one purity annotation should ever be
   * present on a given executable element at a time.
   *
   * @param methodElt a method element
   * @return the purity annotation, or null if none has yet been inferred
   */
  private @Nullable AnnotationMirror getPurityAnnotation(ExecutableElement methodElt) {
    AnnotationMirrorSet declAnnos = storage.getMethodDeclarationAnnotations(methodElt);
    if (declAnnos.isEmpty()) {
      return null;
    }
    for (AnnotationMirror declAnno : declAnnos) {
      if (isPurityAnno(declAnno)) {
        return declAnno;
      }
    }
    return null;
  }

  /**
   * Returns true if the given annotation is {@link Pure}, {@link SideEffectFree}, {@link
   * Deterministic}, or {@link Impure}. Returns false otherwise.
   *
   * @param anno an annotation
   * @return true iff the annotation is a purity annotation
   */
  private boolean isPurityAnno(AnnotationMirror anno) {
    return AnnotationUtils.areSameByName(anno, PURE_NAME)
        || AnnotationUtils.areSameByName(anno, SIDE_EFFECT_FREE_NAME)
        || AnnotationUtils.areSameByName(anno, DETERMINISTIC_NAME)
        || AnnotationUtils.areSameByName(anno, IMPURE_NAME);
  }

  @Override
  public void addFieldDeclarationAnnotation(VariableElement field, AnnotationMirror anno) {
    if (!ElementUtils.isElementFromSourceCode(field)) {
      return;
    }

    String file = storage.getFileForElement(field);
    boolean isNewAnnotation = storage.addFieldDeclarationAnnotation(field, anno);
    if (isNewAnnotation) {
      storage.setFileModified(file);
    }
  }

  @Override
  public void addDeclarationAnnotationToFormalParameter(
      ExecutableElement methodElt, @Positive int index_1based, AnnotationMirror anno) {
    if (index_1based == 0) {
      throw new TypeSystemError(
          "0 is illegal as index argument to addDeclarationAnnotationToFormalParameter");
    }
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    String file = storage.getFileForElement(methodElt);
    boolean isNewAnnotation =
        storage.addDeclarationAnnotationToFormalParameter(methodElt, index_1based, anno);
    if (isNewAnnotation) {
      storage.setFileModified(file);
    }
  }

  @Override
  public void addClassDeclarationAnnotation(TypeElement classElt, AnnotationMirror anno) {
    if (!ElementUtils.isElementFromSourceCode(classElt)) {
      return;
    }

    String file = storage.getFileForElement(classElt);
    boolean isNewAnnotation = storage.addClassDeclarationAnnotation(classElt, anno);
    if (isNewAnnotation) {
      storage.setFileModified(file);
    }
  }

  /**
   * Updates the set of annotations in a location in a program.
   *
   * <ul>
   *   <li>If there was no previous annotation for that location, then the updated set will be the
   *       annotations in rhsATM.
   *   <li>If there was a previous annotation, the updated set will be the LUB between the previous
   *       annotation and rhsATM.
   * </ul>
   *
   * <p>Subclasses can customize this behavior.
   *
   * @param annotationsToUpdate the type whose annotations are modified by this method
   * @param defLoc the location where the annotation will be added
   * @param rhsATM the RHS of the annotated type on the source code
   * @param lhsATM the LHS of the annotated type on the source code
   * @param file the annotation file containing the executable; used for marking the scene as
   *     modified (needing to be written to disk)
   */
  protected void updateAnnotationSet(
      T annotationsToUpdate,
      TypeUseLocation defLoc,
      AnnotatedTypeMirror rhsATM,
      AnnotatedTypeMirror lhsATM,
      String file) {
    updateAnnotationSet(annotationsToUpdate, defLoc, rhsATM, lhsATM, file, true);
  }

  /**
   * Updates the set of annotations in a location in a program.
   *
   * <ul>
   *   <li>If there was no previous annotation for that location, then the updated set will be the
   *       annotations in rhsATM.
   *   <li>If there was a previous annotation, the updated set will be the LUB between the previous
   *       annotation and rhsATM.
   * </ul>
   *
   * <p>Subclasses can customize this behavior.
   *
   * @param annotationsToUpdate the type whose annotations are modified by this method
   * @param defLoc the location where the annotation will be added
   * @param rhsATM the RHS of the annotated type on the source code
   * @param lhsATM the LHS of the annotated type on the source code
   * @param file annotation file containing the executable; used for marking the scene as modified
   *     (needing to be written to disk)
   * @param ignoreIfAnnotated if true, don't update any type that is explicitly annotated in the
   *     source code
   */
  protected void updateAnnotationSet(
      T annotationsToUpdate,
      TypeUseLocation defLoc,
      AnnotatedTypeMirror rhsATM,
      AnnotatedTypeMirror lhsATM,
      String file,
      boolean ignoreIfAnnotated) {
    if (rhsATM instanceof AnnotatedNullType && ignoreNullAssignments) {
      return;
    }

    AnnotatedTypeMirror atmFromStorage =
        storage.atmFromStorageLocation(rhsATM.getUnderlyingType(), annotationsToUpdate);
    updateAtmWithLub(rhsATM, atmFromStorage);

    // For type variables, infer primary annotations for field type use locations, but
    // for other locations only infer primary annotations if they are a super type of the upper
    // bound of declaration of the type variable.
    if (defLoc != TypeUseLocation.FIELD && lhsATM instanceof AnnotatedTypeVariable) {
      AnnotatedTypeVariable lhsTV = (AnnotatedTypeVariable) lhsATM;
      AnnotatedTypeMirror decl =
          atypeFactory.getAnnotatedType(lhsTV.getUnderlyingType().asElement());
      AnnotationMirrorSet upperAnnos = decl.getEffectiveAnnotations();

      // If the inferred type is a subtype of the upper bounds of the
      // current type in the source code, do nothing.
      TypeMirror rhsTM = rhsATM.getUnderlyingType();
      TypeMirror declTM = decl.getUnderlyingType();
      QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
      for (AnnotationMirror anno : rhsATM.getPrimaryAnnotations()) {
        AnnotationMirror upperAnno = qualHierarchy.findAnnotationInSameHierarchy(upperAnnos, anno);
        if (qualHierarchy.isSubtypeShallow(anno, rhsTM, upperAnno, declTM)) {
          rhsATM.removePrimaryAnnotation(anno);
        }
      }
      if (rhsATM.getPrimaryAnnotations().isEmpty()) {
        return;
      }
    }
    storage.updateStorageLocationFromAtm(
        rhsATM, lhsATM, annotationsToUpdate, defLoc, ignoreIfAnnotated);
    storage.setFileModified(file);
  }

  /**
   * Prints a debugging message about a failed inference. Must only be called after {@link
   * #showWpiFailedInferences} has been checked, to avoid constructing the debugging message
   * eagerly.
   *
   * @param reason a message describing the reason an inference was unsuccessful, which will be
   *     displayed to the user
   */
  private void printFailedInferenceDebugMessage(String reason) {
    assert showWpiFailedInferences;
    // TODO: it would be nice if this message also included a line number
    // for the file being analyzed, but I don't know how to get that information
    // here, given that this message is called from places where only the annotated
    // type mirrors for the LHS and RHS of some pseduo-assignment are available.
    System.out.println("WPI failed to make an inference: " + reason);
  }

  @Override
  public void updateAtmWithLub(AnnotatedTypeMirror sourceCodeATM, AnnotatedTypeMirror ajavaATM) {

    if (sourceCodeATM.getKind() != ajavaATM.getKind()) {
      // Ignore null types: passing them to asSuper causes a crash, as they cannot be
      // substituted for type variables. If sourceCodeATM is a null type, only the primary
      // annotation will be considered anyway, so there is no danger of recursing into
      // typevar bounds.
      if (sourceCodeATM.getKind() != TypeKind.NULL) {
        // This can happen e.g. when recursing into the bounds of a type variable:
        // the bound on sourceCodeATM might be a declared type (such as T), while
        // the ajavaATM might be a typevar (such as S extends T), or vice-versa. In
        // that case, use asSuper to make the two ATMs fully-compatible.
        sourceCodeATM = AnnotatedTypes.asSuper(this.atypeFactory, sourceCodeATM, ajavaATM);
      }
    }

    switch (sourceCodeATM.getKind()) {
      case TYPEVAR:
        updateAtmWithLub(
            ((AnnotatedTypeVariable) sourceCodeATM).getLowerBound(),
            ((AnnotatedTypeVariable) ajavaATM).getLowerBound());
        updateAtmWithLub(
            ((AnnotatedTypeVariable) sourceCodeATM).getUpperBound(),
            ((AnnotatedTypeVariable) ajavaATM).getUpperBound());
        break;
      case WILDCARD:
        break;
        // throw new BugInCF("This can't happen");
        // TODO: This comment is wrong: the wildcard case does get entered.
        // Because inferring type arguments is not supported, wildcards won't be
        // encountered.
        // updateATMWithLUB(
        //         atf,
        //         ((AnnotatedWildcardType) sourceCodeATM).getExtendsBound(),
        //         ((AnnotatedWildcardType) ajavaATM).getExtendsBound());
        // updateATMWithLUB(
        //         atf,
        //         ((AnnotatedWildcardType) sourceCodeATM).getSuperBound(),
        //         ((AnnotatedWildcardType) ajavaATM).getSuperBound());
        // break;
      case ARRAY:
        AnnotatedTypeMirror sourceCodeComponent =
            ((AnnotatedArrayType) sourceCodeATM).getComponentType();
        AnnotatedTypeMirror ajavaComponent = ((AnnotatedArrayType) ajavaATM).getComponentType();
        if (sourceCodeComponent.getKind() == ajavaComponent.getKind()) {
          updateAtmWithLub(sourceCodeComponent, ajavaComponent);
        } else {
          if (showWpiFailedInferences) {
            printFailedInferenceDebugMessage(
                String.join(
                    System.lineSeparator(),
                    "attempted to update the component type of an array type, but found an"
                        + " unexpected difference in type structure.",
                    "LHS kind: " + sourceCodeComponent.getKind(),
                    "RHS kind: " + ajavaComponent.getKind()));
            break;
          }
        }
        break;
        // case DECLARED:
        // Inferring annotations on type arguments is not supported, so no need to recur on
        // generic types. If this was ever implemented, this method would need a
        // VisitHistory object to prevent infinite recursion on types such as T extends
        // List<T>.
      default:
        // ATM only has primary annotations
        break;
    }

    // LUB primary annotations
    AnnotationMirrorSet annosToReplace = new AnnotationMirrorSet();
    for (AnnotationMirror amSource : sourceCodeATM.getPrimaryAnnotations()) {
      AnnotationMirror amAjava = ajavaATM.getPrimaryAnnotationInHierarchy(amSource);
      // amAjava only contains annotations from the ajava file, so it might be missing
      // an annotation in the hierarchy.
      if (amAjava != null) {
        amSource =
            atypeFactory
                .getQualifierHierarchy()
                .leastUpperBoundShallow(
                    amSource,
                    sourceCodeATM.getUnderlyingType(),
                    amAjava,
                    ajavaATM.getUnderlyingType());
      }
      annosToReplace.add(amSource);
    }
    sourceCodeATM.replaceAnnotations(annosToReplace);
  }

  @Override
  public void writeResultsToFile(OutputFormat outputFormat, BaseTypeChecker checker) {
    storage.writeResultsToFile(outputFormat, checker);
  }

  @Override
  public void preprocessClassTree(ClassTree classTree) {
    storage.preprocessClassTree(classTree);
  }
}
