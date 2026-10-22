package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Implementation of type argument inference. */
public class DefaultTypeArgumentInference implements TypeArgumentInference {

  /** Stack of all inference problems currently being solved. */
  private final ArrayDeque<InvocationTypeInference> java8InferenceStack = new ArrayDeque<>();

  /** Creates a DefaultTypeArgumentInference. */
  public DefaultTypeArgumentInference() {}

  @SuppressWarnings("interning:not.interned")
  @Override
  public InferenceResult inferTypeArgs(
      AnnotatedTypeFactory typeFactory,
      ExpressionTree expressionTree,
      AnnotatedExecutableType executableType) {
    TreePath pathToExpression = typeFactory.getPath(expressionTree);
    assert pathToExpression != null : "@AssumeAssertion(nullness): the tree is being type-checked";

    // In order to find the type arguments for expressionTree, type arguments for outer method
    // calls may need be inferred, too.
    // So, first find the outermost tree that is required to infer the type arguments for
    // expressionTree
    ExpressionTree outerTree =
        outerInference(
            expressionTree, pathToExpression.getParentPath(), typeFactory.getProcessingEnv());

    for (InvocationTypeInference i : java8InferenceStack) {
      if (i.getInferenceExpression() == outerTree) {
        // Inference is running and is asking for the type of the method before type
        // arguments are substituted. So don't infer any type arguments.  This happens when
        // getting the type of a lambda's returned expression.
        List<Variable> instantiated = new ArrayList<>();
        Theta m = i.context.maps.get(expressionTree);
        if (m == null) {
          return InferenceResult.emptyResult();
        }
        m.values()
            .forEach(
                var -> {
                  if (var.getInstantiation() != null) {
                    instantiated.add(var);
                  }
                });
        if (instantiated.isEmpty()) {
          return InferenceResult.emptyResult();
        }
        return new InferenceResult(instantiated, false, false, "");
      }
    }
    // Null if outerTree is a member reference.
    AnnotatedExecutableType outerMethodType;
    if (outerTree != expressionTree) {
      if (outerTree instanceof MethodInvocationTree mit) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType = typeFactory.methodFromUseWithoutTypeArgInference(mit).executableType();
      } else if (outerTree instanceof NewClassTree nct) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType =
            typeFactory.constructorFromUseWithoutTypeArgInference(nct).executableType();
      } else if (outerTree instanceof MemberReferenceTree) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType = null;
      } else {
        throw new BugInCF(
            "Unexpected kind of outer expression to infer type arguments: %s", outerTree.getKind());
      }
    } else {
      outerMethodType = executableType;
    }

    assert pathToExpression != null : "@AssumeAssertion(nullness): the tree is being type-checked";
    try {
      InvocationTypeInference java8Inference =
          new InvocationTypeInference(typeFactory, pathToExpression, true);
      if (outerTree instanceof MemberReferenceTree outerMemberRef) {
        return inferOnStack(java8Inference, () -> java8Inference.infer(outerMemberRef));
      }
      assert outerMethodType != null
          : "@AssumeAssertion(nullness): outerTree is not a member reference";
      InferenceResult result =
          inferOnStack(java8Inference, () -> java8Inference.infer(outerTree, outerMethodType));
      // The inference problem whose result is used.
      InvocationTypeInference chosenInference = java8Inference;
      if (result.inferenceFailed() && java8Inference.canContextTargetTypeCauseFailure()) {
        // Inference failed, possibly only because of the constraint against the target type of
        // outerTree.  Retry without that constraint, as if outerTree were not a poly expression.
        // Use the retry's result if it succeeds and the type it infers for outerTree has the Java
        // type that the target type requires but qualifiers that the target type does not
        // permit.  Then the check of the assignment, return, argument, etc. that gives outerTree
        // its target type determines whether the type of outerTree is compatible with its
        // context, and reporting "type.arguments.not.inferred" as well would duplicate any error
        // that the check reports.  Furthermore, the visitor skips the other checks of an
        // invocation whose type arguments are not inferred, such as the checks of its arguments.
        // The instantiation that the retry finds satisfies every other constraint, so it is a
        // legitimate typing of outerTree and of every invocation nested within it.
        //
        // The check at the context does not necessarily report an error, because the target type
        // that inference uses can be stricter than the type that the check uses.  For example,
        // when a local variable's type is a type variable, the target type of its initializer
        // lacks the local variable's default qualifiers (see
        // InferenceFactory#assignedToVariable), but the check of the initialization uses them.
        // In that case, the failure of the first attempt is spurious.
        //
        // The retry is used only under these conditions:
        //  * The target type comes from the context of outerTree.  A target type from
        //    AnnotatedTypeFactory#getDummyAssignedTo is not checked at the use site.
        //  * The type that the retry infers for outerTree is assignable to the target type
        //    according to its Java type but not according to its qualifiers (see
        //    ProperType#isAssignable).  Otherwise, the first attempt failed for a reason other
        //    than the incompatibility of that type with the target type, and the check at the
        //    context would not detect that reason.
        //  * The retry infers the same Java types as the first attempt.  The first attempt uses
        //    the target type as javac does, and it failed only because of qualifiers (a failure
        //    because of Java types throws FalseBoundException), so it instantiated every
        //    inference variable.  If the Java types differ, then the target type is needed to
        //    infer the Java types (for example, when it is the only source of the type of a
        //    lambda parameter or of a lambda's return expression), and the retry does not type
        //    the invocation as javac does.
        //
        // Only the target type of outerTree is dropped.  The constraints between a nested
        // invocation (or lambda or method reference) and the formal parameter that it is an
        // argument to remain: the inference variables of outerTree are inferred from them, and
        // dropping them would change the inference of outerTree, not just of the nested
        // expression.  If they are unsatisfiable, then inference fails regardless of the target
        // type of outerTree, and "type.arguments.not.inferred" is reported.
        InvocationTypeInference retry =
            new InvocationTypeInference(typeFactory, pathToExpression, false);
        InferenceResult retryResult =
            inferWithoutTargetType(typeFactory, retry, outerTree, outerMethodType);
        if (retryResult != null
            && !retryResult.inferenceFailed()
            && retry.isQualifiersIncompatibleWithTarget()
            && haveSameJavaTypes(
                result, retryResult, typeFactory.getProcessingEnv().getTypeUtils())) {
          chosenInference = retry;
          result = retryResult;
        }
      }
      chosenInference.recordLambdaParameterTypes();
      if (!result.getResults().containsKey(expressionTree)
          && expressionTree instanceof MemberReferenceTree mrt) {
        // The inference of the outer tree did not create any inference variables for the
        // MemberReferenceTree,
        // so its type arguments still need to be inferred. Reuse the same inference problem
        // because the target type of the MemberReferenceTree
        // was already inferred.
        //
        // This can happen in two cases:
        //
        //  1. By the time the constraint formula for expressionTree was reduced, its target
        //     type had become a proper type, so JLS 18.2.1's rule for
        //     <MethodReference -> T> (which requires that T mention an inference variable)
        //     did not apply.  This happens when the target type's function type has a
        //     parameter type that mentions one of the outer tree's inference variables: JLS
        //     18.5.2.2 makes such a variable an input variable of the constraint, so it is
        //     resolved and substituted into the constraint before the constraint is reduced.
        //
        //  2. The target type's function type has result void, in which case JLS 18.2.1 says
        //     that <MethodReference -> T> reduces to true.

        TreePath pathToMemberRef = typeFactory.getPath(expressionTree);
        assert pathToMemberRef != null
            : "@AssumeAssertion(nullness): the tree is being type-checked";
        InvocationTypeInference mrtInference = chosenInference;
        mrtInference.context.setPathToExpression(pathToMemberRef);
        return inferOnStack(mrtInference, () -> mrtInference.infer(mrt));
      }
      return result.swapTypeVariables(executableType, expressionTree);
    } catch (Exception ex) {
      if (typeFactory
          .getChecker()
          .getBooleanOption("convertTypeArgInferenceCrashToWarning", true)) {
        // This should never happen, if javac infers type arguments so should the Checker
        // Framework. However, given how buggy javac inference is, this probably will, so
        // deal with it gracefully.
        return new InferenceResult(
            Collections.emptyList(),
            false,
            true,
            true,
            "An exception occurred: " + ex.getLocalizedMessage());
      }
      throw BugInCF.addLocation(outerTree, ex);
    }
  }

  /**
   * Returns true if the two inference results infer type arguments for the same trees and type
   * variables, and the Java types of corresponding type arguments are the same.
   *
   * @param result1 an inference result
   * @param result2 an inference result
   * @param types the type utilities
   * @return true if the two results have the same Java types
   */
  private static boolean haveSameJavaTypes(
      InferenceResult result1, InferenceResult result2, Types types) {
    Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> results1 = result1.getResults();
    Map<Tree, Map<TypeVariable, AnnotatedTypeMirror>> results2 = result2.getResults();
    if (!results1.keySet().equals(results2.keySet())) {
      return false;
    }
    for (Map.Entry<Tree, Map<TypeVariable, AnnotatedTypeMirror>> entry : results1.entrySet()) {
      Map<TypeVariable, AnnotatedTypeMirror> typeArgs1 = entry.getValue();
      Map<TypeVariable, AnnotatedTypeMirror> typeArgs2 = results2.get(entry.getKey());
      assert typeArgs2 != null : "@AssumeAssertion(nullness): the key sets are equal";
      if (!typeArgs1.keySet().equals(typeArgs2.keySet())) {
        return false;
      }
      for (Map.Entry<TypeVariable, AnnotatedTypeMirror> typeArg1 : typeArgs1.entrySet()) {
        AnnotatedTypeMirror typeArg2 = typeArgs2.get(typeArg1.getKey());
        assert typeArg2 != null : "@AssumeAssertion(nullness): the key sets are equal";
        if (!isSameTypeModuloCapture(
            typeArg1.getValue().getUnderlyingType(), typeArg2.getUnderlyingType(), types)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns true if the two types are the same, treating two captured type variables as the same if
   * they capture the same wildcard. Each inference problem may create its own captured type
   * variables, which {@link Types#isSameType} considers distinct.
   *
   * @param type1 a type
   * @param type2 a type
   * @param types the type utilities
   * @return true if the two types are the same, modulo capture
   */
  private static boolean isSameTypeModuloCapture(
      @Nullable TypeMirror type1, @Nullable TypeMirror type2, Types types) {
    if (type1 == null || type2 == null) {
      return type1 == null && type2 == null;
    }
    if (types.isSameType(type1, type2)) {
      return true;
    }
    if (type1.getKind() != type2.getKind()) {
      return false;
    }
    switch (type1.getKind()) {
      case TYPEVAR -> {
        WildcardType wildcard1 = TypesUtils.getCapturedWildcard((TypeVariable) type1);
        WildcardType wildcard2 = TypesUtils.getCapturedWildcard((TypeVariable) type2);
        return wildcard1 != null
            && wildcard2 != null
            && isSameTypeModuloCapture(wildcard1, wildcard2, types);
      }
      case DECLARED -> {
        DeclaredType declared1 = (DeclaredType) type1;
        DeclaredType declared2 = (DeclaredType) type2;
        List<? extends TypeMirror> typeArgs1 = declared1.getTypeArguments();
        List<? extends TypeMirror> typeArgs2 = declared2.getTypeArguments();
        if (!types.isSameType(types.erasure(type1), types.erasure(type2))
            || typeArgs1.size() != typeArgs2.size()
            || !isSameTypeModuloCapture(
                declared1.getEnclosingType(), declared2.getEnclosingType(), types)) {
          return false;
        }
        for (int i = 0; i < typeArgs1.size(); i++) {
          if (!isSameTypeModuloCapture(typeArgs1.get(i), typeArgs2.get(i), types)) {
            return false;
          }
        }
        return true;
      }
      case ARRAY -> {
        return isSameTypeModuloCapture(
            ((ArrayType) type1).getComponentType(), ((ArrayType) type2).getComponentType(), types);
      }
      case WILDCARD -> {
        WildcardType wildcard1 = (WildcardType) type1;
        WildcardType wildcard2 = (WildcardType) type2;
        return isSameTypeModuloCapture(
                wildcard1.getExtendsBound(), wildcard2.getExtendsBound(), types)
            && isSameTypeModuloCapture(wildcard1.getSuperBound(), wildcard2.getSuperBound(), types);
      }
      default -> {
        return false;
      }
    }
  }

  /**
   * Performs the inference problem {@code retry}, which ignores the target type of {@code
   * outerTree}.
   *
   * <p>Returns null if inference fails because of the Java types. That is an expected outcome: the
   * problem is one that javac never solves, and without the target type the Java types might not be
   * inferable, for example when the target type is the only source of the type of a lambda's return
   * expression.
   *
   * <p>Any other exception indicates a bug. It is handled as for inference with the target type: if
   * the {@code convertTypeArgInferenceCrashToWarning} option is false, it is rethrown; otherwise,
   * this returns null, so the failure of inference with the target type is reported.
   *
   * @param typeFactory the type factory
   * @param retry the inference problem to solve; it does not use the target type
   * @param outerTree the invocation whose type arguments are inferred
   * @param outerMethodType the type of {@code outerTree}'s method before type argument substitution
   * @return the result of inference, or null if inference threw an exception
   */
  private @Nullable InferenceResult inferWithoutTargetType(
      AnnotatedTypeFactory typeFactory,
      InvocationTypeInference retry,
      ExpressionTree outerTree,
      AnnotatedExecutableType outerMethodType) {
    try {
      return inferOnStack(retry, () -> retry.infer(outerTree, outerMethodType));
    } catch (FalseBoundException ex) {
      return null;
    } catch (Exception ex) {
      if (typeFactory
          .getChecker()
          .getBooleanOption("convertTypeArgInferenceCrashToWarning", true)) {
        return null;
      }
      throw BugInCF.addLocation(outerTree, ex);
    }
  }

  /**
   * Performs {@code inferenceAction} while {@code inference} is on the stack of inference problems
   * being solved.
   *
   * @param inference the inference problem that {@code inferenceAction} solves
   * @param inferenceAction the computation to perform
   * @return the result of {@code inferenceAction}
   */
  private InferenceResult inferOnStack(
      InvocationTypeInference inference, Supplier<InferenceResult> inferenceAction) {
    java8InferenceStack.push(inference);
    try {
      return inferenceAction.get();
    } finally {
      java8InferenceStack.pop();
    }
  }

  @Override
  public @Nullable AnnotatedTypeMirror getLambdaParameterType(VariableElement param) {
    // Iteration order of an ArrayDeque used as a stack is innermost inference problem first.
    for (InvocationTypeInference inference : java8InferenceStack) {
      AnnotatedTypeMirror result = inference.getLambdaParameterType(param);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the outermost tree required to find the type of {@code tree}.
   *
   * @param tree tree that may need an outer tree to find the type
   * @param parentPath path to the parent of {@code tree} or null if no such parent exists
   * @param env the processing environment
   * @return the outermost tree required to find the type of {@code tree}
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  public static ExpressionTree outerInference(
      ExpressionTree tree, @Nullable TreePath parentPath, ProcessingEnvironment env) {
    if (parentPath == null) {
      return tree;
    }
    if (!TreeUtils.isPolyExpression(tree)) {
      return tree;
    }

    Tree parentTree = parentPath.getLeaf();
    switch (parentTree.getKind()) {
      case PARENTHESIZED:
      case CONDITIONAL_EXPRESSION:
        ExpressionTree outer =
            outerInference((ExpressionTree) parentTree, parentPath.getParentPath(), env);
        if (outer == parentTree) {
          return tree;
        }
        return outer;
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) parentTree;
        if (!methodInvocationTree.getTypeArguments().isEmpty()) {
          return tree;
        }
        ExecutableElement methodElement = TreeUtils.elementFromUse(methodInvocationTree);
        if (methodElement.getTypeParameters().isEmpty()) {
          return tree;
        }

        if (TreeUtils.isRawCall(methodInvocationTree, parentPath, env)) {
          return tree;
        }

        if (argumentNeedsInference(
            methodElement, methodInvocationTree.getArguments(), tree, null)) {
          return outerInference((ExpressionTree) parentTree, parentPath.getParentPath(), env);
        }
        return tree;
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) parentTree;
        if (!newClassTree.getTypeArguments().isEmpty()) {
          return tree;
        }
        ExecutableElement constructor = TreeUtils.elementFromUse(newClassTree);
        // A diamond is never a raw call.  JLS 4.8 says "It is a compile-time error to pass type
        // arguments to a non-static member class or interface of a raw type", and javac rejects a
        // diamond on such a class too, so a diamond invocation that compiles never has an erased
        // constructor type.  The only way `typeOf(newClassTree)` is raw for a diamond is JLS
        // 18.5.2's erasure of the *return* type when unchecked conversion was necessary.  That
        // erasure leaves the constructor's parameter types parameterized, so the type arguments
        // still need to be inferred.
        if (!TreeUtils.isDiamondTree(newClassTree)
            && TypesUtils.isRawCall(TreeUtils.typeOf(newClassTree), constructor, env)) {
          return tree;
        }
        if (argumentNeedsInference(constructor, newClassTree.getArguments(), tree, newClassTree)) {
          return outerInference((ExpressionTree) parentTree, parentPath.getParentPath(), env);
        }
        return tree;
      case RETURN:
        TreePath parentParentPath = parentPath.getParentPath();
        if (parentParentPath.getLeaf() instanceof LambdaExpressionTree) {
          return outerInference(
              (ExpressionTree) parentParentPath.getLeaf(), parentParentPath.getParentPath(), env);
        }
        return tree;
      case CASE:
      case YIELD:
        TreePath switchExpressionPath =
            TreePathUtil.pathTillOfKind(parentPath, Kind.SWITCH_EXPRESSION);
        if (switchExpressionPath == null) {
          // The case or yield is in a switch statement rather than a switch expression, so
          // there is no outer inference.
          return tree;
        }
        parentPath = switchExpressionPath;
        parentTree = parentPath.getLeaf();
      // parentTree is a switch expression, so fall through
      case SWITCH_EXPRESSION:
        ExpressionTree outerTree =
            outerInference((ExpressionTree) parentTree, parentPath.getParentPath(), env);
        if (outerTree == parentTree) {
          return tree;
        }
        return outerTree;

      default:
        return tree;
    }
  }

  /**
   * Returns true if {@code argTree} is pseudo-assigned to a parameter in {@code executableElement}
   * that contains a type variable that needs to be inferred.
   *
   * @param executableElement symbol of method or constructor
   * @param argTrees all the arguments of the method or constructor call
   * @param argTree the argument of interest
   * @param newClassTree the new class tree or {@code null} if {@code executableElement} is a method
   * @return true if {@code argTree} is pseudo-assigned to a parameter in {@code executableElement}
   *     that contains a type variable that needs to be inferred
   */
  private static boolean argumentNeedsInference(
      ExecutableElement executableElement,
      List<? extends ExpressionTree> argTrees,
      Tree argTree,
      @Nullable NewClassTree newClassTree) {
    int index = -1;
    for (int i = 0; i < argTrees.size(); i++) {
      @SuppressWarnings("interning") // looking for exact argTree.
      boolean found = argTrees.get(i) == argTree;
      if (found) {
        index = i;
        break;
      }
    }
    if (index == -1) {
      // This happens for an invocation of an inner constructor:
      // var x = new Issue6839<>(1). new Inner<>(1);
      return false;
    }

    ExecutableType executableType = (ExecutableType) executableElement.asType();
    // There are fewer parameters than arguments if this is a varargs method.
    if (executableType.getParameterTypes().size() <= index) {
      index = executableType.getParameterTypes().size() - 1;
    }
    TypeMirror param = executableType.getParameterTypes().get(index);

    if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
      List<TypeVariable> list = new ArrayList<>(executableType.getTypeVariables());
      if (newClassTree != null && TreeUtils.isDiamondTree(newClassTree)) {
        DeclaredType declaredType =
            (DeclaredType) ((DeclaredType) TreeUtils.typeOf(newClassTree)).asElement().asType();
        for (TypeMirror typeVar : declaredType.getTypeArguments()) {
          list.add((TypeVariable) typeVar);
        }
      }
      return ContainsInferenceVariable.hasAnyTypeVariable(list, param);
    }
    return ContainsInferenceVariable.hasAnyTypeVariable(executableType.getTypeVariables(), param);
  }
}
