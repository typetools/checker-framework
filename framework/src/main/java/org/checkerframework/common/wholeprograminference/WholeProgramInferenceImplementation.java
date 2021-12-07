package org.checkerframework.common.wholeprograminference;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.Analysis;
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
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

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
 * @param <T> the type used by the storage to store annotations. See {@link
 *     WholeProgramInferenceStorage}
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

  /** The storage for the inferred annotations. */
  private WholeProgramInferenceStorage<T> storage;

  /** Whether to ignore assignments where the rhs is null. */
  private final boolean ignoreNullAssignments;

  /**
   * Constructs a new {@code WholeProgramInferenceImplementation} that has not yet inferred any
   * annotations.
   *
   * @param atypeFactory the associated type factory
   * @param storage the storage used for inferred annotations and for writing output files
   */
  public WholeProgramInferenceImplementation(
      AnnotatedTypeFactory atypeFactory, WholeProgramInferenceStorage<T> storage) {
    this.atypeFactory = atypeFactory;
    this.storage = storage;
    boolean isNullness =
        atypeFactory.getClass().getSimpleName().equals("NullnessAnnotatedTypeFactory");
    this.ignoreNullAssignments = !isNullness;
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

    List<Node> arguments = objectCreationNode.getArguments();
    updateInferredExecutableParameterTypes(constructorElt, arguments);
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
    // When performing WPI on a library, if there are no external calls (only recursive calls), then
    // each iteration of WPI would make the formal parameter types more restrictive, leading to an
    // infinite (or very long) loop.
    //
    // Consider
    //   void myMethod(int x) { ... myMethod(x-1) ... }`
    // On one iteration, if x has type IntRange(to=100), the recursive call's argument has type
    // IntRange(to=99).  If that is the only call to `MyMethod`, then the formal parameter type
    // would be updated.  On the next iteration it would be refined again to @IntRange(to=98), and
    // so forth.  A recursive call should never restrict a formal parameter type.
    if (isRecursiveCall(methodInvNode)) {
      return;
    }

    List<Node> arguments = methodInvNode.getArguments();
    updateInferredExecutableParameterTypes(methodElt, arguments);
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
   */
  private void updateInferredExecutableParameterTypes(
      ExecutableElement methodElt, List<Node> arguments) {

    String file = storage.getFileForElement(methodElt);

    for (int i = 0; i < arguments.size(); i++) {
      Node arg = arguments.get(i);
      Tree argTree = arg.getTree();

      VariableElement ve = methodElt.getParameters().get(i);
      AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
      AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(argTree);
      atypeFactory.wpiAdjustForUpdateNonField(argATM);
      T paramAnnotations =
          storage.getParameterAnnotations(methodElt, i, paramATM, ve, atypeFactory);
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

    // This code handles fields of "this" and method parameters (including the receiver parameter
    // "this"), for now.  In the future, extend it to other expressions.
    TypeElement containingClass = (TypeElement) methodElt.getEnclosingElement();
    ThisReference thisReference = new ThisReference(containingClass.asType());
    ClassName classNameReceiver = new ClassName(containingClass.asType());
    // Fields of "this":
    for (VariableElement fieldElement :
        ElementFilter.fieldsIn(containingClass.getEnclosedElements())) {
      if (atypeFactory.wpiOutputFormat == OutputFormat.JAIF
          && containingClass.getNestingKind().isNested()) {
        // Don't infer facts about fields of inner classes, because IndexFileWriter
        // places the annotations incorrectly on the class declarations.
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
        // The parameter is not in the store, so don't attempt to create a postcondition for it,
        // since anything other than its default type would not be verifiable. (Only postconditions
        // are supported for parameters.)
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
          // declaredType is null when the method being analyzed is a constructor (which doesn't
          // have a receiver).
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

    for (int i = 0; i < overriddenMethod.getParameterTypes().size(); i++) {
      VariableElement ve = methodElt.getParameters().get(i);
      AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(ve);
      AnnotatedTypeMirror argATM = overriddenMethod.getParameterTypes().get(i);
      atypeFactory.wpiAdjustForUpdateNonField(argATM);
      T paramAnnotations =
          storage.getParameterAnnotations(methodElt, i, paramATM, ve, atypeFactory);
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
      return;
    }

    ExecutableElement methodElt = (ExecutableElement) paramElt.getEnclosingElement();

    AnnotatedTypeMirror paramATM = atypeFactory.getAnnotatedType(paramElt);
    AnnotatedTypeMirror argATM = atypeFactory.getAnnotatedType(rhsTree);
    atypeFactory.wpiAdjustForUpdateNonField(argATM);
    int i = methodElt.getParameters().indexOf(paramElt);
    assert i != -1;
    T paramAnnotations =
        storage.getParameterAnnotations(methodElt, i, paramATM, paramElt, atypeFactory);
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
    // compiler-generated temporary variables will have invalid names. Recording facts about fields
    // with invalid names causes jaif-based WPI to crash when reading the .jaif file, and stub-based
    // WPI to generate unparsable stub files.  See
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

      AnnotatedDeclaredType superclassDecl = pair.getKey();
      ExecutableElement overriddenMethodElement = pair.getValue();

      // Don't infer types for code that isn't presented as source.
      if (!ElementUtils.isElementFromSourceCode(overriddenMethodElement)) {
        continue;
      }

      AnnotatedExecutableType overriddenMethod =
          AnnotatedTypes.asMemberOf(
              atypeFactory.getProcessingEnv().getTypeUtils(),
              atypeFactory,
              superclassDecl,
              overriddenMethodElement);

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

    // Do not infer types for library code, only for type-checked source code.
    if (!ElementUtils.isElementFromSourceCode(methodElt)) {
      return;
    }

    String file = storage.getFileForElement(methodElt);
    boolean isNewAnnotation = storage.addMethodDeclarationAnnotation(methodElt, anno);
    if (isNewAnnotation) {
      storage.setFileModified(file);
    }
  }

  @Override
  public void addFieldDeclarationAnnotation(Element field, AnnotationMirror anno) {
    if (!ElementUtils.isElementFromSourceCode(field)) {
      return;
    }

    String file = storage.getFileForElement(field);
    boolean isNewAnnotation = storage.addFieldDeclarationAnnotation(field, anno);
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
    if (lhsATM instanceof AnnotatedTypeVariable) {
      Set<AnnotationMirror> upperAnnos =
          ((AnnotatedTypeVariable) lhsATM).getUpperBound().getEffectiveAnnotations();
      // If the inferred type is a subtype of the upper bounds of the
      // current type in the source code, do nothing.
      if (upperAnnos.size() == rhsATM.getAnnotations().size()
          && atypeFactory.getQualifierHierarchy().isSubtype(rhsATM.getAnnotations(), upperAnnos)) {
        return;
      }
    }
    storage.updateStorageLocationFromAtm(
        rhsATM, lhsATM, annotationsToUpdate, defLoc, ignoreIfAnnotated);
    storage.setFileModified(file);
  }

  /**
   * Updates sourceCodeATM to contain the LUB between sourceCodeATM and ajavaATM, ignoring missing
   * AnnotationMirrors from ajavaATM -- it considers the LUB between an AnnotationMirror am and a
   * missing AnnotationMirror to be am. The results are stored in sourceCodeATM.
   *
   * @param sourceCodeATM the annotated type on the source code; side effected by this method
   * @param ajavaATM the annotated type on the ajava file
   */
  private void updateAtmWithLub(AnnotatedTypeMirror sourceCodeATM, AnnotatedTypeMirror ajavaATM) {

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
        // Because inferring type arguments is not supported, wildcards won't be encountered.
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
        updateAtmWithLub(
            ((AnnotatedArrayType) sourceCodeATM).getComponentType(),
            ((AnnotatedArrayType) ajavaATM).getComponentType());
        break;
        // case DECLARED:
        // Inferring annotations on type arguments is not supported, so no need to recur on generic
        // types. If this was ever implemented, this method would need a VisitHistory object to
        // prevent infinite recursion on types such as T extends List<T>.
      default:
        // ATM only has primary annotations
        break;
    }

    // LUB primary annotations
    Set<AnnotationMirror> annosToReplace = new HashSet<>(sourceCodeATM.getAnnotations().size());
    for (AnnotationMirror amSource : sourceCodeATM.getAnnotations()) {
      AnnotationMirror amAjava = ajavaATM.getAnnotationInHierarchy(amSource);
      // amAjava only contains annotations from the ajava file, so it might be missing
      // an annotation in the hierarchy.
      if (amAjava != null) {
        amSource = atypeFactory.getQualifierHierarchy().leastUpperBound(amSource, amAjava);
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
