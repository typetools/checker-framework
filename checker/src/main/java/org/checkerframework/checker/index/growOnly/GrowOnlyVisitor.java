package org.checkerframework.checker.index.growOnly;

import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.index.qual.GrowOnly;
import org.checkerframework.checker.index.qual.Shrinkable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the GrowOnly Checker.
 *
 * <p>Issues an error if a method that may shrink a collection (like a method annotated as
 * {@code @Shrinkable}) is called on a reference that is annotated as {@code @GrowOnly}.
 */
public class GrowOnlyVisitor extends BaseTypeVisitor<GrowOnlyAnnotatedTypeFactory> {

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
   * collections. This is the primary type rule for this checker.
   *
   * @param node the method invocation tree
   * @param p an unused parameter
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    // Get the receiver of the method call (e.g., `myList` in `myList.remove(0)`).
    AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(node);

    // only care about method calls on receivers that are explicitly @GrowOnly.
    if (receiverType != null && receiverType.hasPrimaryAnnotation(GrowOnly.class)) {

      // Get the element for the method being called. Checks
      // its annotations from the source or stub files.
      ExecutableElement methodElement = TreeUtils.elementFromUse(node);
      if (methodElement == null) {
        return super.visitMethodInvocation(node, p);
      }

      // Get the full annotated signature of the method declaration.
      AnnotatedExecutableType methodDeclarationType = atypeFactory.getAnnotatedType(methodElement);

      // Get the receiver type that the method *declares* it requires.
      // For `remove()`, this will be @Shrinkable because of our jdk.astub file.
      AnnotatedTypeMirror requiredReceiverType = methodDeclarationType.getReceiverType();

      // The core check: If the method requires a @Shrinkable receiver,
      // then it's an error because we already know our receiver is @GrowOnly.
      if (requiredReceiverType.hasPrimaryAnnotation(Shrinkable.class)) {
        String methodName = TreeUtils.getMethodName(node).toString();
        checker.reportError(node, "mutable.collection.shrink", methodName);
      }
    }

    return super.visitMethodInvocation(node, p);
  }
}
