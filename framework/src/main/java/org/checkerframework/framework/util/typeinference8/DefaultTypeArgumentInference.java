package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayDeque;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference.TypeArgumentInference;
import org.checkerframework.framework.util.typeinference8.util.FalseBoundException;
import org.checkerframework.javacutil.BugInCF;

public class DefaultTypeArgumentInference implements TypeArgumentInference {

  /** Current inference problem that is being solved. */
  private InvocationTypeInference java8Inference = null;
  /** Stack of all inference problems currently being solved. */
  private final ArrayDeque<InvocationTypeInference> java8InferenceStack = new ArrayDeque<>();

  @SuppressWarnings("interning:not.interned")
  @Override
  public InferenceResult inferTypeArgs(
      AnnotatedTypeFactory typeFactory,
      ExpressionTree expressionTree,
      AnnotatedExecutableType methodType) {
    TreePath pathToExpression = typeFactory.getPath(expressionTree);
    ExpressionTree outerTree =
        InvocationTypeInference.outerInference(expressionTree, pathToExpression.getParentPath());
    for (InvocationTypeInference i : java8InferenceStack) {
      if (i.getContext().pathToExpression.getLeaf() == outerTree) {
        // Inference is running and is asking for the type of the method before type arguments are
        // substituted. So don't infer any type arguments.  This happens when getting the type of a
        // lambda's returned expression.
        return InferenceResult.emptyResult();
      }
    }
    if (outerTree != expressionTree) {
      if (outerTree.getKind() == Tree.Kind.METHOD_INVOCATION) {
        pathToExpression = typeFactory.getPath(outerTree);
        methodType =
            typeFactory.methodFromUseNoTypeArgInfere((MethodInvocationTree) outerTree)
                .executableType;
      } else if (outerTree.getKind() == Tree.Kind.NEW_CLASS) {
        pathToExpression = typeFactory.getPath(outerTree);
        methodType =
            typeFactory.constructorFromUseNoTypeArgInfere((NewClassTree) outerTree).executableType;
      } else {
        throw new BugInCF(
            "Unexpected kind of outer expression to infer type arguments: %s", outerTree.getKind());
      }
    }
    if (java8Inference != null) {
      java8InferenceStack.push(java8Inference);
    }
    try {
      java8Inference = new InvocationTypeInference(typeFactory, pathToExpression);
      return java8Inference.infer(outerTree, methodType);
    } catch (FalseBoundException ex) {
      // TODO: For now, rethrow the exception so that the tests crash.
      // This should never happen, if javac infers type arguments so should the Checker
      // Framework. However, given how buggy javac inference is, this probably will, so deal with it
      // gracefully.
      //      checker.reportError(invocation, "type.inference.failed");
      throw ex;
      //      return null;
    } finally {
      if (!java8InferenceStack.isEmpty()) {
        java8Inference = java8InferenceStack.pop();
      } else {
        java8Inference = null;
      }
    }
  }
}
