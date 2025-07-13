package org.checkerframework.checker.index.growOnly;

import com.sun.source.tree.MethodInvocationTree;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Mutable Index Checker.
 *
 * <p>Issues an error if a method that may shrink a collection (like {@code remove} or {@code
 * clear}) is called on a reference that is annotated as {@code @GrowOnly}.
 *
 * <p>It also handles the subtyping checks for assignments and overrides automatically, thanks to
 * inheriting from {@link BaseTypeVisitor}.
 */
public class GrowOnlyVisitor extends BaseTypeVisitor<GrowOnlyAnnotatedTypeFactory> {

  /** A set of method names that are disallowed on @GrowOnly collections. */
  private static final Set<String> SHRINKING_METHODS =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList("remove", "removeAll", "removeIf", "retainAll", "clear")));

  /**
   * Creates a new GrowOnlyVisitor.
   *
   * @param checker the checker that created this visitor
   */
  public GrowOnlyVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Checks method invocations to prevent shrinking methods from being called on @GrowOnly
   * collections.
   *
   * @param node the method invocation tree
   * @param p an unused parameter
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    String methodName = TreeUtils.getMethodName(node).toString();

    // Check if this method is one that can shrink a collection.
    if (methodName != null && SHRINKING_METHODS.contains(methodName)) {
      // Get the receiver of the method call (e.g., `myList` in `myList.remove(0)`).
      AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(node);

      if (receiverType == null) {
        return super.visitMethodInvocation(node, p);
      }

      // Check if the receiver is annotated as @GrowOnly.
      if (receiverType.hasPrimaryAnnotation(GrowOnly.class)) {
        checker.reportError(node, "mutable.collection.shrink", methodName);
      }
    }

    return super.visitMethodInvocation(node, p);
  }
}
