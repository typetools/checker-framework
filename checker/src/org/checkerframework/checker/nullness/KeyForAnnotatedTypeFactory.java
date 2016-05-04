package org.checkerframework.checker.nullness;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import org.checkerframework.checker.nullness.KeyForPropagator.PropagationDirection;
import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.PolyKeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.typeinference.DefaultTypeArgumentInference;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.TypeArgumentInference;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

public class KeyForAnnotatedTypeFactory extends
    GenericAnnotatedTypeFactory<CFValue, CFStore, KeyForTransfer, KeyForAnalysis> {

    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR;

    private final KeyForPropagator keyForPropagator;
    private final KeyForCanonicalizer keyForCanonicalizer = new KeyForCanonicalizer();

    protected final Class<? extends Annotation> checkerKeyForClass = org.checkerframework.checker.nullness.qual.KeyFor.class;

    /** Regular expression for an identifier */
    protected final String identifierRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";

    /** Matches an identifier */
    protected final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");

    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        KEYFOR = AnnotationUtils.fromClass(elements, KeyFor.class);
        UNKNOWNKEYFOR = AnnotationUtils.fromClass(elements, UnknownKeyFor.class);
        keyForPropagator = new KeyForPropagator(UNKNOWNKEYFOR);

        // Add compatibility annotations:
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForDecl.class, KEYFOR);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForType.class, KEYFOR);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<Class<? extends Annotation>>(
                        Arrays.asList(KeyFor.class, UnknownKeyFor.class, KeyForBottom.class,
                                PolyKeyFor.class, PolyAll.class)));
    }

    @Override
    protected TypeArgumentInference createTypeArgumentInference() {
        return new KeyForTypeArgumentInference();
    }

    /* TODO: we currently do not substitute field types.
   * postAsMemberOf only gives us the type of the receiver expression ("owner"),
   * but not the Tree. Therefore, we could not decide the substitution.
   * I think it shouldn't happen frequently to have a field
   * with annotation @KeyFor("this").
   * However, one field being marked as the key for a different field might
   * be necessary, so changing a @KeyFor("map") into @KeyFor("recv.map")
   * might be necessary.
  @Override
  protected void postAsMemberOf(AnnotatedTypeMirror type,
      AnnotatedTypeMirror owner, Element element) {
  }
    */

  // TODO
  /* Once the method substitution is stable, create
   * substituteNewClass. Look whether they can share code somehow
   * (the two classes don't share a common interface).
  @Override
  public AnnotatedExecutableType constructorFromUse(NewClassTree call) {
    assert call != null;

    AnnotatedExecutableType constructor = super.constructorFromUse(call);

    Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

    // Get the result type
    AnnotatedTypeMirror resultType = getAnnotatedType(call);

    // Modify parameters
    for (AnnotatedTypeMirror parameterType : constructor.getParameterTypes()) {
      AnnotatedTypeMirror combinedType = substituteNewClass(call, parameterType);
      mappings.put(parameterType, combinedType);
    }

    // TODO: upper bounds, throws?

    constructor = constructor.substitute(mappings);

    return constructor;
  }
  */

  @Override
  public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> constructorFromUse(NewClassTree tree) {
      Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> result = super.constructorFromUse(tree);

      final AnnotatedTypeMirror returnType = result.first.getReturnType();

      //Can we square this with the KEyForPropagationTreeAnnotator
      Pair<Tree, AnnotatedTypeMirror> context = getVisitorState().getAssignmentContext();

      if (returnType.getKind() == TypeKind.DECLARED && context != null && context.first != null) {
          AnnotatedTypeMirror assignedTo = TypeArgInferenceUtil.assignedTo(this, getPath(tree));

          if (assignedTo != null) {
              //array types and boxed primitives etc don't require propagation
              if (assignedTo.getKind() == TypeKind.DECLARED) {
                  final AnnotatedDeclaredType newClassType = (AnnotatedDeclaredType) returnType;
                  keyForPropagator.propagate(newClassType, (AnnotatedDeclaredType) assignedTo,
                          PropagationDirection.TO_SUBTYPE, this);
              }
          }
      }

      return result;
  }

  // TODO: doc
  @Override
  public Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> methodFromUse(MethodInvocationTree call) {
    assert call != null;
    // System.out.println("looking at call: " + call);
    Pair<AnnotatedExecutableType, List<AnnotatedTypeMirror>> mfuPair = super.methodFromUse(call);
    AnnotatedExecutableType method = mfuPair.first;
    ExecutableElement methElem = method.getElement();
    AnnotatedExecutableType declMethod = this.getAnnotatedType(methElem);

    Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mappings = new HashMap<>();

    // Modify parameters
    List<AnnotatedTypeMirror> params = method.getParameterTypes();
    List<AnnotatedTypeMirror> declParams = declMethod.getParameterTypes();
    assert params.size() == declParams.size();

    for (int i = 0; i < params.size(); ++i) {
      AnnotatedTypeMirror param = params.get(i);
      AnnotatedTypeMirror subst = substituteCall(call, declParams.get(i), param);
      mappings.put(param, subst);
    }

    // Modify return type
    AnnotatedTypeMirror returnType = method.getReturnType();
    if (returnType.getKind() != TypeKind.VOID ) {
      AnnotatedTypeMirror subst = substituteCall(call, declMethod.getReturnType(), returnType);
      mappings.put(returnType, subst);
    }

    method = (AnnotatedExecutableType) AnnotatedTypeReplacer.replace(method, mappings);

    // System.out.println("adapted method: " + method);

    return Pair.of(method, mfuPair.second);
  }

  /* TODO: doc
   * This pattern and the logic how to use it is copied from NullnessFlow.
   * NullnessFlow already contains four exact copies of the logic for handling this
   * pattern and should really be refactored.
   */
  private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");

  // TODO: copied from NullnessFlow, but without the "." at the end.
  private String receiver(MethodInvocationTree node) {
      ExpressionTree sel = node.getMethodSelect();
      if (sel.getKind() == Tree.Kind.IDENTIFIER) {
          return "";
      } else if (sel.getKind() == Tree.Kind.MEMBER_SELECT) {
         return ((MemberSelectTree)sel).getExpression().toString();
      }
      ErrorReporter.errorAbort("KeyForAnnotatedTypeFactory.receiver: cannot be here");
      return null; // dead code
  }

  // TODO: doc
  // TODO: "this" should be implicitly prepended
  // TODO: substitutions also need to be applied to argument types
  private AnnotatedTypeMirror substituteCall(MethodInvocationTree call,
          AnnotatedTypeMirror declInType, AnnotatedTypeMirror inType) {

      // System.out.println("input type: " + inType);
      AnnotatedTypeMirror outType = inType.shallowCopy();

      AnnotationMirror anno = declInType.getAnnotation(KeyFor.class);
      if (anno != null) {

          List<String> inMaps = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
          List<String> outMaps = new ArrayList<String>();

         String receiver = receiver(call);

         for (String inMapName : inMaps) {
             if (parameterPtn.matcher(inMapName).matches()) {
                 int param = Integer.valueOf(inMapName.substring(1));
                 if (param <= 0 || param > call.getArguments().size()) {
                     // The failure should already have been reported, when the
                     // method declaration was processed.
                     // checker.report(Result.failure("param.index.nullness.parse.error", inMapName), call);
                 } else {
                     String res = call.getArguments().get(param-1).toString();
                     outMaps.add(res);
                 }
             } else if (inMapName.equals("this")) {
                 outMaps.add(receiver);
             } else {
                 // TODO: look at the code below, copied from NullnessFlow
                 // System.out.println("KeyFor argument unhandled: " + inMapName + " using " + receiver + "." + inMapName);
                 // do not always add the receiver, e.g. for local variables this creates a mess
                 // outMaps.add(receiver + "." + inMapName);
                 // just copy name for now, better than doing nothing
                 outMaps.add(inMapName);
             }
             // TODO: look at code in NullnessFlow and decide whether there
             // are more cases to copy.
         }

         AnnotationBuilder builder = new AnnotationBuilder(processingEnv, KeyFor.class);
         builder.setValue("value", outMaps);
         AnnotationMirror newAnno =  builder.build();

         outType.removeAnnotation(KeyFor.class);
         outType.addAnnotation(newAnno);
      }

      if (declInType.getKind() == TypeKind.DECLARED &&
              outType.getKind() == TypeKind.DECLARED) {
          AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) outType;
          AnnotatedDeclaredType declDeclaredType = (AnnotatedDeclaredType) declInType;
          Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<>();

          List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
          List<AnnotatedTypeMirror> declTypeArgs = declDeclaredType.getTypeArguments();

          assert typeArgs.size() == declTypeArgs.size();

          // Get the substituted type arguments
          for (int i = 0; i < typeArgs.size(); ++i) {
              AnnotatedTypeMirror typeArgument = typeArgs.get(i);
              AnnotatedTypeMirror substTypeArgument = substituteCall(call, declTypeArgs.get(i), typeArgument);
              mapping.put(typeArgument, substTypeArgument);
          }

          outType = AnnotatedTypeReplacer.replace(declaredType, mapping);
      } else if (declInType.getKind() == TypeKind.ARRAY &
              outType.getKind() == TypeKind.ARRAY) {
          AnnotatedArrayType arrayType = (AnnotatedArrayType) outType;
          AnnotatedArrayType declArrayType = (AnnotatedArrayType) declInType;

          // Get the substituted component type
          AnnotatedTypeMirror elemType = arrayType.getComponentType();
          AnnotatedTypeMirror substElemType = substituteCall(call, declArrayType.getComponentType(), elemType);

          arrayType.setComponentType(substElemType);
          // outType aliases arrayType
      } else if (outType.getKind().isPrimitive() ||
              outType.getKind() == TypeKind.WILDCARD ||
              outType.getKind() == TypeKind.TYPEVAR) {
          // TODO: for which of these should we also recursively substitute?
          // System.out.println("KeyForATF: Intentionally unhandled Kind: " + outType.getKind());
      } else {
          // System.err.println("KeyForATF: Unknown getKind(): " + outType.getKind());
          // assert false;
      }

      // System.out.println("result type: " + outType);
      return outType;
  }

  @Override
  protected TypeHierarchy createTypeHierarchy() {
      return new KeyForTypeHierarchy(checker, getQualifierHierarchy(),
                                     checker.getOption("ignoreRawTypeArguments", "true").equals("true"),
                                     checker.hasOption("invariantArrays"));
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
      return new ListTreeAnnotator(
             new PropagationTreeAnnotator(this),
             new ImplicitsTreeAnnotator(this) ,
             new KeyForPropagationTreeAnnotator(this, keyForPropagator)
      );
  }

  protected class KeyForTypeHierarchy extends DefaultTypeHierarchy {

      public KeyForTypeHierarchy(BaseTypeChecker checker, QualifierHierarchy qualifierHierarchy,
                                 boolean ignoreRawTypes, boolean invariantArrayComponents) {
          super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
      }

      @Override
      public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {

          //TODO: THIS IS FROM THE OLD TYPE HIERARCHY.  WE SHOULD FIX DATA-FLOW/PROPAGATION TO DO THE RIGHT THING
          if (supertype.getKind() == TypeKind.TYPEVAR &&
              subtype.getKind() == TypeKind.TYPEVAR) {
              // TODO: Investigate whether there is a nicer and more proper way to
              // get assignments between two type variables working.
              if (supertype.getAnnotations().isEmpty()) {
                  return true;
              }
          }

          // Otherwise Covariant would cause trouble.
          if (subtype.hasAnnotation(KeyForBottom.class)) {
              return true;
          }
          return super.isSubtype(subtype, supertype, visited);
      }


      protected boolean isCovariant(final int typeArgIndex, final int[] covariantArgIndexes) {
          if (covariantArgIndexes != null) {
              for (int covariantIndex : covariantArgIndexes) {
                  if (typeArgIndex == covariantIndex) {
                      return true;
                  }
              }
          }

          return false;
      }

      @Override
      public Boolean visitTypeArgs(AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype,
                                      VisitHistory visited,  boolean subtypeIsRaw, boolean supertypeIsRaw) {
          final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeIsRaw || supertypeIsRaw);

          if (!ignoreTypeArgs) {

              //TODO: Make an option for honoring this annotation in DefaultTypeHierarchy?
              final TypeElement supertypeElem = (TypeElement) supertype.getUnderlyingType().asElement();
              int[] covariantArgIndexes = null;
              if (supertypeElem.getAnnotation(Covariant.class) != null) {
                  covariantArgIndexes = supertypeElem.getAnnotation(Covariant.class).value();
              }

              final List<? extends AnnotatedTypeMirror> subtypeTypeArgs   = subtype.getTypeArguments();
              final List<? extends AnnotatedTypeMirror> supertypeTypeArgs = supertype.getTypeArguments();

              if ( subtypeTypeArgs.isEmpty() || supertypeTypeArgs.isEmpty() ) {
                  return true;
              }

              if (supertypeTypeArgs.size() > 0) {
                  for (int i = 0; i < supertypeTypeArgs.size(); i++) {
                      final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
                      final AnnotatedTypeMirror subTypeArg   = subtypeTypeArgs.get(i);

                      if (subtypeIsRaw || supertypeIsRaw) {
                          rawnessComparer.isValidInHierarchy(subtype, supertype, currentTop, visited);
                      } else {
                          if (!isContainedBy(subTypeArg, superTypeArg, visited,
                                  isCovariant(i, covariantArgIndexes))) {
                              return false;
                          }
                      }
                  }
              }
          }

          return true;
      }
  }

  /*
   * Given a string array 'values', returns an AnnotationMirror corresponding to @KeyFor(values)
   */
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(LinkedHashSet<String> values) {
      // Create an AnnotationBuilder with the ArrayList
      AnnotationBuilder builder =
              new AnnotationBuilder(getProcessingEnv(), KeyFor.class);
      builder.setValue("value", values.toArray());

      // Return the resulting AnnotationMirror
      return builder.build();
  }

  /*
   * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
   */
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(String value) {
      // Create an ArrayList with the value
      LinkedHashSet<String> values = new LinkedHashSet<String>();
      values.add(value);
      return createKeyForAnnotationMirrorWithValue(values);
  }

  /*
   * This method uses FlowExpressionsParseUtil to attempt to recognize the
   * variable names indicated in the values in KeyFor(values).
   *
   * This method modifies atm such that the values are replaced with the
   * string representation of the Flow Expression Receiver returned by
   * FlowExpressionsParseUtil.parse. This ensures that when comparing KeyFor
   * values later when doing subtype checking that equivalent expressions
   * (such as "field" and "this.field" when there is no local variable
   * "field") are represented by the same string so that string comparison
   * will succeed.
   *
   * This is necessary because when KeyForTransfer generates KeyFor
   * annotations, it uses FlowExpressions to generate the values in
   * KeyFor(values). canonicalizeKeyForValues ensures that user-provided
   * KeyFor annotations will contain values that match the format of those in
   * the generated KeyFor annotations.
   *
   * Returns null if the values did not change.
   *
   */
  private LinkedHashSet<String> canonicalizeKeyForValues(AnnotationMirror anno,
          FlowExpressionContext flowExprContext, TreePath path, Tree t,
          boolean returnNullIfUnchanged) {

      if (anno != null) {
          boolean unknownReceiver = flowExprContext.receiver == null || flowExprContext.receiver.containsUnknown();
          boolean valuesChanged = false; // Indicates that at least one value was changed in the list.
          LinkedHashSet<String> newValues = new LinkedHashSet<String>();

          List<String> values = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
          for (String s: values) {
              Receiver varTypeReceiver = null;

              try {
                  varTypeReceiver = FlowExpressionParseUtil.parse(s, flowExprContext, path);
              } catch (FlowExpressionParseException e) {
                  // Canonicalization is a best-effort approach and it should not cause an exception
                  // to be thrown if an expression cannot be parsed. If an expression that must be
                  // canonicalized cannot be because of an inability to parse it, a type checking
                  // error will be issued later if the expression is compared to its canonical equivalent.
                  // For example, if @KeyFor("#1") could not be canonicalized to @KeyFor("var1"), and
                  // it is later compared to a @KeyFor("var1") annotation that was written by the user,
                  // a type checking error will result.
              }

              if (unknownReceiver // The receiver type was unknown initially, and ...
                      && (varTypeReceiver == null
                      || varTypeReceiver.containsUnknown()) // ... the receiver type is still unknown after a call to parse
                      ) {
                  // parse did not find a static member field. Try a nonstatic field.

                  try {
                      // Try a field in the current object. Do not modify s itself since
                      // it is used in the newValue.equals(s) check below.
                      varTypeReceiver = FlowExpressionParseUtil.parse("this." + s,
                              flowExprContext, path);
                  } catch (FlowExpressionParseException e) {
                      // See the comment in the "catch (FlowExpressionParseException e)" block above.
                  }
              }

              if (varTypeReceiver != null) {
                  String newValue = varTypeReceiver.toString();
                  newValues.add(newValue);

                  if (!newValue.equals(s)) {
                      valuesChanged = true;
                  }
              } else {
                  newValues.add(s); // This will get ignored if valuesChanged is false after exiting the for loop
              }
          }

          if (!returnNullIfUnchanged || valuesChanged) {
              // There is no need to sort the resulting array because the subtype
              // check will be a containsAll call, not an equals call.
              return newValues;
          }
      }

      return null;
  }

  // Returns null if the AnnotationMirror did not change.
  private AnnotationMirror canonicalizeKeyForValuesGetAnnotationMirror(AnnotationMirror anno,
          FlowExpressionContext flowExprContext, TreePath path, Tree t) {
      LinkedHashSet<String> newValues = canonicalizeKeyForValues(anno, flowExprContext, path, t, true);

      return newValues == null ? null : createKeyForAnnotationMirrorWithValue(newValues);
  }

  private void canonicalizeKeyForValues(AnnotatedTypeMirror atm, FlowExpressionContext flowExprContext,
          TreePath path, Tree t) {

      AnnotationMirror anno = canonicalizeKeyForValuesGetAnnotationMirror(atm.getAnnotation(KeyFor.class),
              flowExprContext, path, t);

      if (anno != null) {
          atm.replaceAnnotation(anno);
      }
  }

  private void keyForCanonicalizeValuesForMethodCall(AnnotatedTypeMirror varType,
          AnnotatedTypeMirror valueType,
          Tree t,
          TreePath path,
          Node node) {

      assert(node instanceof MethodInvocationNode || node instanceof ObjectCreationNode);

      /* The following code is best explained by example. Suppose we have the following:

      public static class Graph {
          private Map<String, Integer> adjList = new HashMap<String, Integer>();
          public static boolean addEdge(@KeyFor("#2.adjList") String theStr, Graph theGraph) {
              ...
          }
      }

      public static class TestClass {
          public void buildGraph(Graph myGraph, @KeyFor("#1.adjList") String myStr) {
              Graph.addEdge(myStr, myGraph);
          }
      }

      The challenge is to recognize that in the call to addEdge(myStr, myGraph), myGraph
      corresponds to theGraph formal parameter, even though one is labeled as
      parameter #1 and the other as #2.

      All we know at this point is:
      -We have a varType whose annotation is @KeyFor("#2.adjList")
      -We have a valueType whose annotation is @KeyFor("#1.adjList")
      -We are processing a method call Graph.addEdge(myStr, myGraph)

      We need to build flow expression contexts that will allow us
      to convert both annotations into @KeyFor("myGraph.adjList")
      so that we will know they are equivalent.
      */

      // Building the context for the varType is straightforward. We need it to be
      // the context of the call site (Graph.addEdge(myStr, myGraph)) so that the
      // formal parameters theStr and theGraph will be replaced with the actual
      // parameters myStr and myGraph. The call to
      // keyForCanonicalizer.canonicalize(varType,flowExprContextVarType,path,t);
      // will then be able to transform "#2.adjList" into "myGraph.adjList"
      // since myGraph is the second actual parameter in the call.

      if (varType != null) {
          FlowExpressionContext flowExprContextVarType =
              node instanceof MethodInvocationNode ?
              FlowExpressionParseUtil.buildFlowExprContextForUse((MethodInvocationNode) node, getContext()) :
              FlowExpressionParseUtil.buildFlowExprContextForUse((ObjectCreationNode) node, path, getContext());

          keyForCanonicalizer.canonicalize(varType,flowExprContextVarType,path,t);
      }

      if (valueType != null) {
          FlowExpressionContext flowExprContextValueType = null;

          // Building the context for the valueType is more subtle. That's because
          // at the call site of Graph.addEdge(myStr, myGraph), we no longer have
          // any notion of what parameter #1 refers to. That information is found
          // at the declaration of the enclosing method.

          MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);

          if (enclosingMethod != null) {

              // An important piece of information when creating the Flow Context
              // is the receiver. If the enclosing method is static, we need the
              // receiver to be the class name (e.g. Graph). Otherwise we need
              // the receiver to be the instance of the class (e.g. someGraph,
              // if the call were someGraph.myMethod(...)

              // To be able to generate the receiver, we need the enclosing class.

              ClassTree enclosingClass = TreeUtils.enclosingClass(path);

              Node receiver = null;
              if (enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC)) {
                  receiver = new ClassNameNode(enclosingClass);
              } else {
                  receiver = new ImplicitThisLiteralNode(InternalUtils.typeOf(enclosingClass));
              }

              Receiver internalReceiver = FlowExpressions.internalReprOf(this, receiver);

              // Now we need to translate the method parameters. #1.adjList needs to
              // become myGraph.adjList. We do not do that translation here, as that
              // is handled by the call to keyForCanonicalizer.canonicalize(valueType, ...) below.
              // However, we indicate that the actual parameters are [myGraph, myStr]
              // so that keyForCanonicalizer.canonicalize can translate #1 to myGraph.

              List<Receiver> internalArguments = new ArrayList<>();

              // Note that we are not handling varargs as we assume that parameter
              // numbers such as "#2" cannot refer to a vararg expanded argument.

              for (VariableTree vt : enclosingMethod.getParameters()) {
                  internalArguments.add(FlowExpressions.internalReprOf(this,
                          new LocalVariableNode(vt, receiver)));
              }

              // Create the Flow Expression context in terms of the receiver and parameters.

              flowExprContextValueType = new FlowExpressionContext(internalReceiver, internalArguments, getContext());
          } else {

              // If there is no enclosing method, then we are probably dealing with a field initializer.
              // In that case, we do not need to worry about transforming parameter numbers such as #1
              // since they are meaningless in this context. Create the usual Flow Expression context
              // as the context of the call site.

              flowExprContextValueType =
                  node instanceof MethodInvocationNode ?
                  FlowExpressionParseUtil.buildFlowExprContextForUse((MethodInvocationNode) node, getContext()) :
                  FlowExpressionParseUtil.buildFlowExprContextForUse((ObjectCreationNode) node, path, getContext());
          }

          keyForCanonicalizer.canonicalize(valueType,flowExprContextValueType,path,t);
      }
  }

  /*
   * Verifies only that the primary @KeyFor annotation on the RHS is a subtype
   * of the primary @KeyFor annotation on the LHS. Useful for determining if
   * the RHS is a @KeyFor for at least the maps in the LHS. For a full subtype
   * check, please refer to KeyForQualifierHierarchy.isSubType. If changing
   * the subtyping logic here, be sure to also change it there.
   */
  public boolean keyForValuesSubtypeCheck(AnnotationMirror varType, // Notice that the varType is an AM while the valueType is an ATM
          AnnotatedTypeMirror valueType,
          Tree t,
          MethodInvocationNode node
          ) {
      TreePath path = getPath(t);

      FlowExpressionContext flowExprContextVarType =
          FlowExpressionParseUtil.buildFlowExprContextForUse(node, getContext());

      LinkedHashSet<String> var = canonicalizeKeyForValues(varType, flowExprContextVarType, path, t, false);

      keyForCanonicalizeValuesForMethodCall(null, valueType, t, path, node);

      AnnotationMirror keyForAnnotationMirrorValueType = valueType.getAnnotation(KeyFor.class);

      List<String> val = keyForAnnotationMirrorValueType == null ?
              null :
              AnnotationUtils.getElementValueArray(keyForAnnotationMirrorValueType, "value", String.class, false);

      if (var == null && val == null) {
          return true;
      } else if (var == null || val == null) {
          return false;
      }

      return val.containsAll(var);
  }

  public void keyForCanonicalizeValues(AnnotatedTypeMirror varType,
          AnnotatedTypeMirror valueType, TreePath path) {

      Tree t = path.getLeaf();

      Node node = getNodeForTree(t);

      if (node != null) {
          if (node instanceof MethodInvocationNode ||
              node instanceof ObjectCreationNode) {
              keyForCanonicalizeValuesForMethodCall(varType, valueType, t, path, node);
          } else {
              Receiver r = FlowExpressions.internalReprOf(this, node);

              List<Receiver> internalArguments = null;

              MethodTree enclosingMethod = TreeUtils.enclosingMethod(path);

              if (enclosingMethod != null) {
                  ClassTree enclosingClass = TreeUtils.enclosingClass(path);

                  Node receiver = null;
                  if (enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC)) {
                      receiver = new ClassNameNode(enclosingClass);
                  } else {
                      receiver = new ImplicitThisLiteralNode(InternalUtils.typeOf(enclosingClass));
                  }

                  internalArguments = new ArrayList<>();

                  // Note that we are not handling varargs as we assume that parameter
                  // numbers such as "#2" cannot refer to a vararg expanded argument.

                  for (VariableTree vt : enclosingMethod.getParameters()) {
                      internalArguments.add(FlowExpressions.internalReprOf(this,
                              new LocalVariableNode(vt, receiver)));
                  }
              }

              FlowExpressionContext flowExprContext = new FlowExpressionContext(r, internalArguments, getContext());

              keyForCanonicalizer.canonicalize(varType,flowExprContext,path,t);
              keyForCanonicalizer.canonicalize(valueType,flowExprContext,path,t);
          }
      }
  }

  class KeyForCanonicalizer extends AnnotatedTypeScanner<Void, Void> {
    private FlowExpressionContext context = null;
    private TreePath path = null;
    private Tree leaf = null;

    // An instance of KeyForCanonicalizer can be reused because canonicalize calls reset().
    protected  void canonicalize(final AnnotatedTypeMirror type, final FlowExpressionContext context,
                                 final TreePath path, final Tree leaf) {
        reset();

        this.context = context;
        this.path = path;
        this.leaf = leaf;
        this.scan(type, null);
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void v) {
      if (type == null) {
          return null;             //handles non-existent receivers
      }

      canonicalizeKeyForValues(type, context, path, leaf);
      return super.scan(type, null);
    }
  }

  @Override
  public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
      return new KeyForQualifierHierarchy(factory);
  }

  private final class KeyForQualifierHierarchy extends GraphQualifierHierarchy {

      public KeyForQualifierHierarchy(MultiGraphFactory factory) {
          super(factory, null);
      }

      @Override
      public AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
          AnnotationMirror top = getTopAnnotation(start);

          if (AnnotationUtils.areSameIgnoringValues(top, UNKNOWNKEYFOR)) {
              return null;
          }

          if (polyQualifiers.containsKey(top)) {
              return polyQualifiers.get(top);
          } else if (polyQualifiers.containsKey(polymorphicQualifier)) {
              return polyQualifiers.get(polymorphicQualifier);
          } else {
              // No polymorphic qualifier exists for that hierarchy.
              ErrorReporter.errorAbort("GraphQualifierHierarchy: did not find the polymorphic qualifier corresponding to qualifier " + start +
                      "; all polymorphic qualifiers: " + polyQualifiers  + "; this: " + this);
              return null;
          }
      }

      /*
       * Note that KeyForAnnotatedTypeFactory.keyForValuesSubtypeCheck does a similar subtype check
       * for a specific scenario. If changing the subtyping logic here, be sure to also change it there.
       */
      @Override
      public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
          if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR) &&
              AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
              List<String> lhsValues = null;
              List<String> rhsValues = null;

              Map<? extends ExecutableElement, ? extends AnnotationValue> valMap = lhs.getElementValues();

              if (valMap.isEmpty()) {
                  lhsValues = new ArrayList<String>();
              } else {
                  lhsValues = AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);
              }

              valMap = rhs.getElementValues();

              if (valMap.isEmpty()) {
                  rhsValues = new ArrayList<String>();
              } else {
                  rhsValues = AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);
              }

              return rhsValues.containsAll(lhsValues);
          }
          // Ignore annotation values to ensure that annotation is in supertype map.
          if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR)) {
              lhs = KEYFOR;
          }
          if (AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
              rhs = KEYFOR;
          }
          return super.isSubtype(rhs, lhs);
      }
  }


  /**
   *  A TypeArgumentInference implementation that canonicalizes keyfor values.
   */
  class KeyForTypeArgumentInference extends DefaultTypeArgumentInference {

      @Override
      public void adaptMethodType(AnnotatedTypeFactory typeFactory, ExpressionTree invocation,
              AnnotatedExecutableType methodType) {
          canonicalizeForViewpointAdaptation(invocation, methodType);
      }

      @Override
      protected List<AnnotatedTypeMirror> getArgumentTypes(ExpressionTree expression,
                                                           AnnotatedTypeFactory typeFactory) {
          final List<AnnotatedTypeMirror> argTypes = super.getArgumentTypes(expression, typeFactory);
          if (!argTypes.isEmpty()) {
              final TreePath pathToInvocation = getPath(expression).getParentPath();
              TreePath enclosingMethodPath = TreeUtils.pathTillOfKind(pathToInvocation, Kind.METHOD);

              Node node = getNodeForTree(expression);

              if (node == null || enclosingMethodPath == null) {
                  return argTypes;
              }

              FlowExpressionContext flowExpressionContext =
                  FlowExpressionParseUtil.buildFlowExprContextForViewpointUse(node, pathToInvocation,
                                                                              enclosingMethodPath, getContext());


              for (AnnotatedTypeMirror argType : argTypes) {
                  keyForCanonicalizer.canonicalize(argType, flowExpressionContext, pathToInvocation, expression);
              }
          }

          return argTypes;
      }

      @Override
      protected AnnotatedTypeMirror getAssignedTo(ExpressionTree expression, AnnotatedTypeFactory typeFactory) {
          final AnnotatedTypeMirror assignedTo = super.getAssignedTo(expression, typeFactory);

          if (assignedTo != null) {
              canonicalizeForViewpointAdaptation(expression, assignedTo);
          }

          return assignedTo;
      }
  }

  /**
   * Immediately before AnnotatedTypes.findTypeArguments we canonicalize flow
   * expressions using the parameters for the current method (not the one
   * being invoked but the one in which it is contained) and otherwise the
   * flow expression context from the invocation expression.
   */
  public void canonicalizeForViewpointAdaptation(ExpressionTree invocation, AnnotatedTypeMirror type) {
      final TreePath path = getPath(invocation);
      TreePath enclosingMethodPath = TreeUtils.pathTillOfKind(path, Kind.METHOD);

      if (path == null || enclosingMethodPath == null) {
          return; //this seems to happen for cases of desugaring from Data Flow
      }

      Node node = getNodeForTree(path.getLeaf());

      //node == null means we are still performing flow
      if (node == null || node instanceof FunctionalInterfaceNode) {
          return;
      }

      if (  invocation.getKind() != Kind.METHOD_INVOCATION
              && invocation.getKind() != Kind.NEW_CLASS ) {
          ErrorReporter.errorAbort(
                  "canonicalizeForViewpointAdaptation can only be called on method invocations"
                + "and constructor calls.\n"
                + "tree=" + invocation + "\n"
                + "type=" + type + "\n"
                + "node=" + node + "\n"

          );
      }

      FlowExpressionContext flowExpressionContext =
          FlowExpressionParseUtil.buildFlowExprContextForViewpointUse(node, path, enclosingMethodPath, getContext());
      keyForCanonicalizer.canonicalize(type, flowExpressionContext, path, invocation);
  }
}
