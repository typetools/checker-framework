package org.checkerframework.checker.index;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A collection of utility functions used by several Index Checker subcheckers. */
public class IndexUtil {
  /**
   * Returns true if the type is a sequence supported by this checker.
   *
   * @param type a type
   * @return true if the type is a sequence supported by this checker
   */
  public static boolean isSequenceType(TypeMirror type) {
    return type.getKind() == TypeKind.ARRAY || TypesUtils.isString(type);
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
