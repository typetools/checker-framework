package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.plumelib.util.SystemPlume;

/**
 * {@link TreeAnnotator} is an abstract SimpleTreeVisitor to be used with {@link ListTreeAnnotator}.
 *
 * <p>A TreeAnnotator is mainly intended to change the type of an expression. If a TreeAnnotator
 * changes the type of a variable declaration, that change will not generally be seen at uses of the
 * variable, because uses of a variable tend to obtain the variable's type from an Element (which
 * always exists) rather than from a Tree (which might or might not be available at the time of the
 * variable's use).
 *
 * <p>This class does not visit component parts of the tree. By default, the visit methods all call
 * {@link #defaultAction(Tree, Object)}, which does nothing unless overridden. Therefore, subclass
 * implementations of methods do not need to call {@code super()} unless they override {@link
 * #defaultAction(Tree, Object)}.
 *
 * @see ListTreeAnnotator
 * @see PropagationTreeAnnotator
 * @see LiteralTreeAnnotator
 */
public abstract class TreeAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

  /**
   * Whether to output verbose, low-level debugging messages. Also see {@code
   * GenericAnnotatedTypeFactory.debug}.
   */
  private static final boolean debug = false;

  /** The type factory. */
  protected final AnnotatedTypeFactory atypeFactory;

  /**
   * Create a new TreeAnnotator.
   *
   * @param atypeFactory the type factory
   */
  protected TreeAnnotator(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  /**
   * Output a message, if logging is on.
   *
   * @param format a format string
   * @param args arguments to the format string
   */
  @FormatMethod
  protected void log(String format, Object... args) {
    if (debug) {
      SystemPlume.sleep(1); // logging can interleave with typechecker output
      System.out.printf(format, args);
    }
  }

  /**
   * This method is not called when checking a method invocation against its declaration. So,
   * instead of overriding this method, override TypeAnnotator.visitExecutable.
   * TypeAnnotator.visitExecutable is called both when checking method declarations and method
   * invocations.
   *
   * @see org.checkerframework.framework.type.typeannotator.TypeAnnotator
   */
  @Override
  public Void visitMethod(MethodTree tree, AnnotatedTypeMirror p) {
    return super.visitMethod(tree, p);
  }

  /**
   * When overriding this method, getAnnotatedType on the left and right operands should only be
   * called when <b>absolutely necessary</b>. Otherwise, the checker will be very slow on heavily
   * nested binary trees. (For example, a + b + c + d + e + f + g + h.)
   *
   * <p>One approach is to perform work in this method only if {@code
   * type.hasPrimaryAnnotationInHierarchy(...)} returns false.
   *
   * <p>If a checker's performance is still too slow:
   *
   * <ul>
   *   <li>Compute the types of binary trees in a subclass of {@link
   *       org.checkerframework.framework.flow.CFTransfer}; look up the value in the store rather
   *       than the AnnotatedTypeFactory.
   *   <li>This method ({@code TreeAnnotator.visitBinary}) should annotate binary trees with top so
   *       that the type applied in the transfer is always a subtype of the type the
   *       AnnotatedTypeFactory computes.
   * </ul>
   */
  @Override
  public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror mirror) {
    return super.visitBinary(tree, mirror);
  }
}
