package org.checkerframework.checker.index;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A collection of utility functions used by several Index Checker subcheckers. */
public final class IndexUtil {

  /** Do not instantiate IndexUtil. */
  private IndexUtil() {
    throw new Error("Do not instantiate IndexUtil.");
  }

  /**
   * Returns true if the type is a sequence supported by this checker.
   *
   * @param type a type
   * @return true if the type is a sequence supported by this checker
   */
  public static boolean isSequenceType(TypeMirror type) {
    return type.getKind() == TypeKind.ARRAY || TypesUtils.isString(type);
  }

  /**
   * Returns the type whose primary annotation a tree annotator should change in order to refine
   * {@code type}: the upper bound of a type variable, or {@code type} itself otherwise. Changing
   * the primary annotation of a type variable would also change its lower bound.
   *
   * @param type a type to refine
   * @return the type whose primary annotation to change
   */
  public static AnnotatedTypeMirror typeToRefine(AnnotatedTypeMirror type) {
    while (type instanceof AnnotatedTypeVariable typeVariable) {
      type = typeVariable.getUpperBound();
    }
    return type;
  }

  /** Gets a sequence tree for a length access tree, or null if it is not a length access. */
  public static @Nullable ExpressionTree getLengthSequenceTree(
      Tree lengthTree, IndexMethodIdentifier imf, ProcessingEnvironment processingEnv) {
    if (TreeUtils.isArrayLengthAccess(lengthTree)) {
      return ((MemberSelectTree) lengthTree).getExpression();
    } else if (imf.isLengthOfMethodInvocation(lengthTree)) {
      return TreeUtils.getReceiverTree((MethodInvocationTree) lengthTree);
    }

    return null;
  }
}
