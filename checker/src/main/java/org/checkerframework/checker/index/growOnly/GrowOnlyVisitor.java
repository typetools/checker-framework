package org.checkerframework.checker.index.growOnly;

import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.ExecutableElement;
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
 *
 * <p>It also handles the subtyping checks for assignments and overrides automatically, thanks to
 * inheriting from {@link BaseTypeVisitor}.
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
    if (receiverType == null) {
      return super.visitMethodInvocation(node, p);
    }

    // Get the method's signature from the AST.
    ExecutableElement methodElement = TreeUtils.elementFromUse(node);
    AnnotatedExecutableType methodType = atypeFactory.getAnnotatedType(methodElement);

    // Get the type the method *requires* for its receiver.
    // For a method like `remove()`, this will be @Shrinkable because of our jdk.astub file.
    AnnotatedTypeMirror requiredReceiverType = methodType.getReceiverType();

    // Use the visitor's built-in assignment check to see if the actual receiver
    // (`receiverType`) is a valid subtype of what the method requires (`requiredReceiverType`).
    // This will automatically issue an error with our custom message if the check fails.
    // The first argument is the "variable" (what is required).
    // The second argument is the "value" (what we have).
    commonAssignmentCheck(requiredReceiverType, receiverType, node, "mutable.collection.shrink");

    return super.visitMethodInvocation(node, p);
  }
}
