package org.checkerframework.checker.nullness;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.nullness.qual.Covariant;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ImplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.Result;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeReplacer;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.visitor.VisitHistory;

import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

@TypeQualifiers({ KeyFor.class, UnknownKeyFor.class, KeyForBottom.class})
public class KeyForAnnotatedTypeFactory extends
    GenericAnnotatedTypeFactory<CFValue, CFStore, KeyForTransfer, KeyForAnalysis> {

    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR;

    protected final Class<? extends Annotation> checkerKeyForClass = org.checkerframework.checker.nullness.qual.KeyFor.class;

    protected final /*@CompilerMessageKey*/ String KEYFOR_VALUE_PARAMETER_VARIABLE_NAME = "keyfor.value.parameter.variable.name";
    protected final /*@CompilerMessageKey*/ String KEYFOR_VALUE_PARAMETER_VARIABLE_NAME_FORMAL_PARAM_NUM = "keyfor.value.parameter.variable.name.formal.param.num";

    /** Regular expression for an identifier */
    protected final String identifierRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";

    /** Matches an identifier */
    protected final Pattern identifierPattern = Pattern.compile("^"
            + identifierRegex + "$");

    public KeyForAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);

        KEYFOR = AnnotationUtils.fromClass(elements, KeyFor.class);
        UNKNOWNKEYFOR = AnnotationUtils.fromClass(elements, UnknownKeyFor.class);

        this.postInit();

        this.defaults.addAbsoluteDefault(UNKNOWNKEYFOR, DefaultLocation.ALL);

        // Add compatibility annotations:
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForDecl.class, KEYFOR);
        addAliasedAnnotation(org.checkerframework.checker.nullness.compatqual.KeyForType.class, KEYFOR);
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
     if (sel.getKind() == Tree.Kind.IDENTIFIER)
         return "";
     else if (sel.getKind() == Tree.Kind.MEMBER_SELECT)
         return ((MemberSelectTree)sel).getExpression().toString();
     ErrorReporter.errorAbort("KeyForAnnotatedTypeFactory.receiver: cannot be here");
     return null; // dead code
 }

 // TODO: doc
 // TODO: "this" should be implicitly prepended
 // TODO: substitutions also need to be applied to argument types
 private AnnotatedTypeMirror substituteCall(MethodInvocationTree call, AnnotatedTypeMirror declInType, AnnotatedTypeMirror inType) {

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
         Map<AnnotatedTypeMirror, AnnotatedTypeMirror> mapping = new HashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>();

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
     } else if(outType.getKind().isPrimitive() ||
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
                                     checker.hasOption("ignoreRawTypeArguments"),
                                     checker.hasOption("invariantArrays"));
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
          if(covariantArgIndexes != null) {
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

              if( subtypeTypeArgs.isEmpty() || supertypeTypeArgs.isEmpty() ) {
                  return true;
              }

              if (supertypeTypeArgs.size() > 0) {
                  for (int i = 0; i < supertypeTypeArgs.size(); i++) {
                      final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
                      final AnnotatedTypeMirror subTypeArg   = subtypeTypeArgs.get(i);

                      if(subtypeIsRaw || supertypeIsRaw) {
                          rawnessComparer.isValidInHierarchy(subtype, supertype, currentTop, visited);
                      } else {
                          if (!isContainedBy(subTypeArg, superTypeArg, visited, isCovariant(i, covariantArgIndexes))) {
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
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(ArrayList<String> values) {
      // Create an AnnotationBuilder with the ArrayList

      AnnotationBuilder builder =
              new AnnotationBuilder(getProcessingEnv(), KeyFor.class);
      builder.setValue("value", values);

      // Return the resulting AnnotationMirror

      return builder.build();
  }

  /*
   * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
   */
  public AnnotationMirror createKeyForAnnotationMirrorWithValue(String value) {
      // Create an ArrayList with the value

      ArrayList<String> values = new ArrayList<String>();

      values.add(value);

      return createKeyForAnnotationMirrorWithValue(values);
  }

  /*
   * This method uses FlowExpressionsParseUtil to attempt to recognize the variable names indicated in the values in KeyFor(values).
   *
   * This method modifies atm such that the values are replaced with the string representation of the Flow Expression Receiver
   * returned by FlowExpressionsParseUtil.parse. This ensures that when comparing KeyFor values later when doing subtype checking
   * that equivalent expressions (such as "field" and "this.field" when there is no local variable "field") are represented by the same
   * string so that string comparison will succeed.
   *
   * This is necessary because when KeyForTransfer generates KeyFor annotations, it uses FlowExpressions to generate the values in KeyFor(values).
   * canonicalizeKeyForValues ensures that user-provided KeyFor annotations will contain values that match the format of those in the generated
   * KeyFor annotations.
   *
   * Returns null if the values did not change.
   *
   */
  private ArrayList<String> canonicalizeKeyForValues(AnnotationMirror anno, FlowExpressionContext flowExprContext, TreePath path, Tree t, boolean returnNullIfUnchanged) {
      Receiver varTypeReceiver = null;

      CFAbstractStore<?, ?> store = null;
      boolean unknownReceiver = false;

      if (flowExprContext.receiver.containsUnknown()) {
          // If the receiver is unknown, we will try local variables

          store = getStoreBefore(t);
          unknownReceiver = true; // We could use store != null for this check, but this is clearer.
      }

      if (anno != null) {
          boolean valuesChanged = false; // Indicates that at least one value was changed in the list.
          ArrayList<String> newValues = new ArrayList<String>();

          List<String> values = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
          for (String s: values){
              boolean localVariableFound = false;

              if (unknownReceiver) {
                  // If the receiver is unknown, try a local variable
                  CFAbstractValue<?> val = store.getValueOfLocalVariableByName(s);

                  if (val != null) {
                      newValues.add(s);
                      // Don't set valuesChanged to true since local variable names are already canonicalized
                      localVariableFound = true;
                  }
              }

              if (localVariableFound == false) {
                  try {
                      varTypeReceiver = FlowExpressionParseUtil.parse(s, flowExprContext, path);
                  } catch (FlowExpressionParseException e) {
                  }

                  if (unknownReceiver // The receiver type was unknown initially, and ...
                          && (varTypeReceiver == null
                          || varTypeReceiver.containsUnknown()) // ... the receiver type is still unknown after a call to parse
                          ) {
                      // parse did not find a static member field. Try a nonstatic field.

                      try {
                          varTypeReceiver = FlowExpressionParseUtil.parse("this." + s, // Try a field in the current object. Do not modify s itself since it is used in the newValue.equals(s) check below.
                                  flowExprContext, path);
                      } catch (FlowExpressionParseException e) {
                      }
                  }

                  if (varTypeReceiver != null) {
                      String newValue = varTypeReceiver.toString();
                      newValues.add(newValue);

                      if (!newValue.equals(s)) {
                          valuesChanged = true;
                      }
                  }
                  else {
                      newValues.add(s); // This will get ignored if valuesChanged is false after exiting the for loop
                  }
              }
          }

          if (!returnNullIfUnchanged || valuesChanged) {
              return newValues; // There is no need to sort the resulting array because the subtype check will be a containsAll call, not an equals call.
          }
      }

      return null;
  }

  // Returns null if the AnnotationMirror did not change.
  private AnnotationMirror canonicalizeKeyForValuesGetAnnotationMirror(AnnotationMirror anno, FlowExpressionContext flowExprContext, TreePath path, Tree t) {
      ArrayList<String> newValues = canonicalizeKeyForValues(anno, flowExprContext, path, t, true);

      return newValues == null ? null : createKeyForAnnotationMirrorWithValue(newValues);
  }

  private void canonicalizeKeyForValues(AnnotatedTypeMirror atm, FlowExpressionContext flowExprContext, TreePath path, Tree t) {

      AnnotationMirror anno = canonicalizeKeyForValuesGetAnnotationMirror(atm.getAnnotation(KeyFor.class), flowExprContext, path, t);

      if (anno != null) {
          atm.replaceAnnotation(anno);
      }
  }

  // Build new varType and valueType with canonicalized expressions in the values
  private void keyForCanonicalizeValuesForMethodInvocationNode(AnnotatedTypeMirror varType,
          AnnotatedTypeMirror valueType,
          Tree t,
          TreePath path,
          MethodInvocationNode node) {

      Pair<ArrayList<String>, ArrayList<String>> valuesPair = keyForCanonicalizeValuesForMethodInvocationNode(
              varType.getAnnotation(KeyFor.class),
              valueType.getAnnotation(KeyFor.class),
              t, path, node, true
              );

      ArrayList<String> var = valuesPair.first;
      ArrayList<String> val = valuesPair.second;

      if (var != null) {
          varType.replaceAnnotation(createKeyForAnnotationMirrorWithValue(var));
      }

      if (val != null) {
          valueType.replaceAnnotation(createKeyForAnnotationMirrorWithValue(val));
      }
  }

  /* Deal with the special case where parameters were specified as
     variable names. This is a problem because those variable names are
     ambiguous and could refer to different variables at the call sites.
     Issue a warning to the user if the variable name is a plain identifier
     (with no preceding this. or classname.) */
  private void keyForIssueWarningIfArgumentValuesContainVariableName(List<Receiver> arguments, Tree t, Name methodName, MethodInvocationNode node) {

      ArrayList<String> formalParamNames = null;
      boolean formalParamNamesAreValid = true;

      for(int i = 0; i < arguments.size(); i++) {
          Receiver argument = arguments.get(i);

          List<? extends AnnotationMirror> keyForAnnos = argument.getType().getAnnotationMirrors();
          if (keyForAnnos != null) {
              for(AnnotationMirror anno : keyForAnnos) {
                  if (AnnotationUtils.areSameByClass(anno, checkerKeyForClass)) {
                      List<String> values = AnnotationUtils.getElementValueArray(anno, "value", String.class, false);
                      for (String s: values){
                          Matcher identifierMatcher = identifierPattern.matcher(s);

                          if (identifierMatcher.matches()) {
                              if (formalParamNames == null) { // Lazy initialization
                                  formalParamNames = new ArrayList<String>();
                                  ExecutableElement el = TreeUtils.elementFromUse(node.getTree());
                                  List<? extends VariableElement> varels = el.getParameters();
                                  for(VariableElement varel : varels) {
                                      String formalParamName = varel.getSimpleName().toString();

                                      // Heuristic: if the formal parameter name appears to be synthesized, and not the
                                      // original name, don't bother adding any parameter names to the list.
                                      if (formalParamName.equals("p0") || formalParamName.equals("arg0")) {
                                          formalParamNamesAreValid = false;
                                          break;
                                      }

                                      formalParamNames.add(formalParamName);
                                  }
                              }

                              int formalParamNum = -1;
                              if (formalParamNamesAreValid) {
                                  formalParamNum = formalParamNames.indexOf(s);
                              }

                              String paramNumString = Integer.toString(i + 1);

                              if (formalParamNum == -1) {
                                  checker.report(Result.warning(KEYFOR_VALUE_PARAMETER_VARIABLE_NAME, s, paramNumString, methodName), t);
                              }
                              else {
                                  String formalParamNumString = Integer.toString(formalParamNum + 1);

                                  checker.report(Result.warning(KEYFOR_VALUE_PARAMETER_VARIABLE_NAME_FORMAL_PARAM_NUM, s, paramNumString, methodName, formalParamNumString), t);
                              }
                          }
                      }
                  }
              }
          }
      }
  }

  private Pair<ArrayList<String>, ArrayList<String>> keyForCanonicalizeValuesForMethodInvocationNode(AnnotationMirror varType,
          AnnotationMirror valueType,
          Tree t,
          TreePath path,
          MethodInvocationNode node,
          boolean returnNullIfUnchanged) {

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
      // canonicalizeKeyForValues(varType, flowExprContextVarType, path, t);
      // will then be able to transform "#2.adjList" into "myGraph.adjList"
      // since myGraph is the second actual parameter in the call.

      FlowExpressionContext flowExprContextVarType = FlowExpressionParseUtil.buildFlowExprContextForUse(node, getContext()),
              flowExprContextValueType = null;

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
          }
          else {
              receiver = new ImplicitThisLiteralNode(InternalUtils.typeOf(enclosingClass));
          }

          Receiver internalReceiver = FlowExpressions.internalReprOf(this, receiver);

          // Now we need to translate the method parameters. #1.adjList needs to
          // become myGraph.adjList. We do not do that translation here, as that
          // is handled by the call to canonicalizeKeyForValues(valueType, ...) below.
          // However, we indicate that the actual parameters are [myGraph, myStr]
          // so that canonicalizeKeyForValues can translate #1 to myGraph.

          List<Receiver> internalArguments = new ArrayList<>();

          // Note that we are not handling varargs as we assume that parameter numbers such as "#2" cannot refer to a vararg expanded argument.

          for (VariableTree vt : enclosingMethod.getParameters()) {
              internalArguments.add(FlowExpressions.internalReprOf(this,
                      new LocalVariableNode(vt, receiver)));
          }

          // Create the Flow Expression context in terms of the receiver and parameters.

          flowExprContextValueType = new FlowExpressionContext(internalReceiver, internalArguments, getContext());

          keyForIssueWarningIfArgumentValuesContainVariableName(flowExprContextValueType.arguments, t, enclosingMethod.getName(), node);
      }
      else {

          // If there is no enclosing method, then we are probably dealing with a field initializer.
          // In that case, we do not need to worry about transforming parameter numbers such as #1
          // since they are meaningless in this context. Create the usual Flow Expression context
          // as the context of the call site.

          flowExprContextValueType = FlowExpressionParseUtil.buildFlowExprContextForUse(node, getContext());
      }

      // If they are local variable names, they are already canonicalized. So we only need to canonicalize
      // the names of static and instance fields.

      ArrayList<String> var = canonicalizeKeyForValues(varType, flowExprContextVarType, path, t, returnNullIfUnchanged);
      ArrayList<String> val = canonicalizeKeyForValues(valueType, flowExprContextValueType, path, t, returnNullIfUnchanged);

      return Pair.of(var, val);
  }

  public boolean keyForValuesSubtypeCheck(AnnotationMirror varType,
          AnnotationMirror valueType,
          Tree t,
          MethodInvocationNode node
          ) {
      TreePath path = getPath(t);

      Pair<ArrayList<String>, ArrayList<String>> valuesPair = keyForCanonicalizeValuesForMethodInvocationNode(varType, valueType, t, path, node, false);

      ArrayList<String> var = valuesPair.first;
      ArrayList<String> val = valuesPair.second;

      if (var == null && val == null) {
          return true;
      }
      else if (var == null || val == null) {
          return false;
      }

      return val.containsAll(var);
  }

  public void keyForCanonicalizeValues(AnnotatedTypeMirror varType,
          AnnotatedTypeMirror valueType, TreePath path) {

      Tree t = path.getLeaf();

      Node node = getNodeForTree(t);

      if (node != null) {
          if (node instanceof MethodInvocationNode) {
              keyForCanonicalizeValuesForMethodInvocationNode(varType, valueType, t, path, (MethodInvocationNode) node);
          }
          else {
              Receiver r = FlowExpressions.internalReprOf(this, node);

              FlowExpressionContext flowExprContext = new FlowExpressionContext(r, null, getContext());

              canonicalizeKeyForValues(varType, flowExprContext, path, t);
              canonicalizeKeyForValues(valueType, flowExprContext, path, t);
          }
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
      
      @Override
      public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
          if (AnnotationUtils.areSameIgnoringValues(lhs, KEYFOR) &&
              AnnotationUtils.areSameIgnoringValues(rhs, KEYFOR)) {
              List<String> lhsValues = null;
              List<String> rhsValues = null;

              Map<? extends ExecutableElement, ? extends AnnotationValue> valMap = lhs.getElementValues();

              if (valMap.isEmpty())
                  lhsValues = new ArrayList<String>();
              else
                  lhsValues = AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);

              valMap = rhs.getElementValues();

              if (valMap.isEmpty())
                  rhsValues = new ArrayList<String>();
              else
                  rhsValues = AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);

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


}
