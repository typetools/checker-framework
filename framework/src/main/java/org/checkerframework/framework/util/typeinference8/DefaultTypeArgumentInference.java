package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/** Implementation of type argument inference. */
public class DefaultTypeArgumentInference implements TypeArgumentInference {

  /** Current inference problem that is being solved. */
  private InvocationTypeInference java8Inference = null;

  /** Stack of all inference problems currently being solved. */
  private final ArrayDeque<InvocationTypeInference> java8InferenceStack = new ArrayDeque<>();

  /** Creates a DefaultTypeArgumentInference. */
  public DefaultTypeArgumentInference() {}

  @SuppressWarnings("interning:not.interned")
  @Override
  public InferenceResult inferTypeArgs(
      AnnotatedTypeFactory typeFactory,
      ExpressionTree expressionTree,
      AnnotatedExecutableType methodType) {
    TreePath pathToExpression = typeFactory.getPath(expressionTree);

    // In order to find the type arguments for expressionTree, type arguments for outer method
    // calls may need be inferred, too.
    // So, first find the outermost tree that is required to infer the type arguments for
    // expressionTree
    ExpressionTree outerTree = outerInference(expressionTree, pathToExpression.getParentPath());

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
    AnnotatedExecutableType outerMethodType;
    if (outerTree != expressionTree) {
      if (outerTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType =
            typeFactory.methodFromUseWithoutTypeArgInference((MethodInvocationTree) outerTree)
                .executableType;
      } else if (outerTree.getKind() == Tree.Kind.NEW_CLASS) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType =
            typeFactory.constructorFromUseWithoutTypeArgInference((NewClassTree) outerTree)
                .executableType;
      } else if (outerTree.getKind() == Kind.MEMBER_REFERENCE) {
        pathToExpression = typeFactory.getPath(outerTree);
        outerMethodType = null;
      } else {
        throw new BugInCF(
            "Unexpected kind of outer expression to infer type arguments: %s", outerTree.getKind());
      }
    } else {
      outerMethodType = methodType;
    }
    if (java8Inference != null) {
      java8InferenceStack.push(java8Inference);
    }
    try {
      java8Inference = new InvocationTypeInference(typeFactory, pathToExpression);
      if (outerTree.getKind() == Kind.MEMBER_REFERENCE) {
        return java8Inference.infer((MemberReferenceTree) outerTree);
      } else {
        InferenceResult result = java8Inference.infer(outerTree, outerMethodType);
        if (!result.getResults().containsKey(expressionTree)
            && expressionTree.getKind() == Kind.MEMBER_REFERENCE) {
          java8Inference.context.pathToExpression = typeFactory.getPath(expressionTree);
          return java8Inference.infer((MemberReferenceTree) expressionTree);
        }
        return result.swapTypeVariables(methodType, expressionTree);
      }
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
    } finally {
      if (!java8InferenceStack.isEmpty()) {
        java8Inference = java8InferenceStack.pop();
      } else {
        java8Inference = null;
      }
    }
  }

  /**
   * Returns the outermost tree required to find the type of {@code tree}.
   *
   * @param tree tree that may need an outer tree to find the type
   * @param parentPath path to the parent of {@code tree} or null if no such parent exists
   * @return the outermost tree required to find the type of {@code tree}
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  public static ExpressionTree outerInference(ExpressionTree tree, @Nullable TreePath parentPath) {
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
            outerInference((ExpressionTree) parentTree, parentPath.getParentPath());
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
        if (argumentNeedsInference(
            methodElement, methodInvocationTree.getArguments(), tree, null)) {
          return outerInference((ExpressionTree) parentTree, parentPath.getParentPath());
        }
        return tree;
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) parentTree;
        if (!newClassTree.getTypeArguments().isEmpty()) {
          return tree;
        }
        ExecutableElement constructor = TreeUtils.elementFromUse(newClassTree);
        if (argumentNeedsInference(constructor, newClassTree.getArguments(), tree, newClassTree)) {
          return outerInference((ExpressionTree) parentTree, parentPath.getParentPath());
        }
        return tree;
      case RETURN:
        TreePath parentParentPath = parentPath.getParentPath();
        if (parentParentPath.getLeaf().getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
          return outerInference(
              (ExpressionTree) parentParentPath.getLeaf(), parentParentPath.getParentPath());
        }
        return tree;
      default:
        if (TreeUtils.isYield(parentTree)) {
          parentPath = parentPath.getParentPath();
          // The first parent is a case statement.
          parentPath = parentPath.getParentPath();
          parentTree = parentPath.getLeaf();
        }
        if (TreeUtils.isSwitchExpression(parentTree)) {
          // case SWITCH_EXPRESSION:
          ExpressionTree outerTree =
              outerInference((ExpressionTree) parentTree, parentPath.getParentPath());
          if (outerTree == parentTree) {
            return tree;
          }
          return outerTree;
        }
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
   *     that contains a type variable that needs to be inferred.
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
      }
    }
    if (index == -1) {
      // This happens for an invocation of an inner constructor:
      // var x = new Issue6839<>(1). new Inner<>(1);
      return false;
    }

    ExecutableType executableType = (ExecutableType) executableElement.asType();
    // There are fewer parameters than arguments if this is a var args method.
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
