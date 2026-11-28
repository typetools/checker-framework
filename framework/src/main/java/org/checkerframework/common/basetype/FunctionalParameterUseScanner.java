package org.checkerframework.common.basetype;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.util.PurityChecker;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Determines whether a method body contains a use of a functional-interface parameter that relies
 * on the purity of the argument, in the sense of {@code
 * BaseTypeVisitor.mightRelyOnFunctionalParameter}.
 *
 * <p>To use it, call {@code scan(body, null)} on a fresh scanner, and then read {@link #found}.
 * Scanning has no other effect, and the visit methods return null. Scanning stops early once {@link
 * #found} is true.
 */
class FunctionalParameterUseScanner extends TreeScanner<Void, Void> {

  /** The parameter whose uses are sought. */
  private final VariableElement parameter;

  /** The functional method of the type of {@link #parameter}. */
  private final ExecutableElement parameterFunction;

  /** The processing environment. */
  private final ProcessingEnvironment env;

  /** True if a use that relies on the purity of the argument has been found. */
  boolean found = false;

  /**
   * Creates a scanner for uses of {@code parameter}.
   *
   * @param parameter a functional-interface parameter
   * @param parameterFunction the functional method of {@code parameter}'s type
   * @param env the processing environment
   */
  FunctionalParameterUseScanner(
      VariableElement parameter, ExecutableElement parameterFunction, ProcessingEnvironment env) {
    this.parameter = parameter;
    this.parameterFunction = parameterFunction;
    this.env = env;
  }

  @Override
  public Void scan(@Nullable Tree tree, Void p) {
    return found ? null : super.scan(tree, p);
  }

  /**
   * Scans {@code tree}, unless it is {@link #parameter} itself.
   *
   * @param tree an expression, or null
   */
  private void scanUnlessParameter(@Nullable ExpressionTree tree) {
    if (!isParameter(tree)) {
      scan(tree, null);
    }
  }

  /**
   * Returns true if {@code tree} is a use of {@link #parameter}, possibly in parentheses.
   *
   * @param tree an expression, or null
   * @return true if {@code tree} is a use of {@link #parameter}
   */
  private boolean isParameter(@Nullable ExpressionTree tree) {
    return tree != null
        && TreeUtils.withoutParens(tree) instanceof IdentifierTree identifier
        && Objects.equals(parameter, TreeUtils.elementFromTree(identifier));
  }

  /**
   * {@inheritDoc}
   *
   * <p>The body might not have been attributed yet, as when it is in a top-level class that javac
   * has not yet processed. An identifier that has no element is treated as a use of {@link
   * #parameter} if it has the same name. Such a use is treated as relying on the purity of the
   * argument, even if it is in a context that {@link #isParameter} would otherwise exempt.
   */
  @Override
  public Void visitIdentifier(IdentifierTree tree, Void p) {
    Element elt = TreeUtils.elementFromTree(tree);
    if (elt == null
        ? tree.getName().contentEquals(parameter.getSimpleName())
        : parameter.equals(elt)) {
      found = true;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Overwriting a variable does not read it, so a destination that is a bare identifier is not a
   * use. That is so whether or not the body has been attributed, so the identifier's element is not
   * consulted. A compound destination, such as an array access, is scanned, because its
   * subexpressions are evaluated.
   */
  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    if (!(TreeUtils.withoutParens(tree.getVariable()) instanceof IdentifierTree)) {
      scan(tree.getVariable(), p);
    }
    scanUnlessParameter(tree.getExpression());
    return null;
  }

  @Override
  public Void visitVariable(VariableTree tree, Void p) {
    scan(tree.getModifiers(), p);
    scan(tree.getType(), p);
    scanUnlessParameter(tree.getInitializer());
    return null;
  }

  @Override
  public Void visitReturn(ReturnTree tree, Void p) {
    scanUnlessParameter(tree.getExpression());
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    // A reference is an operand only of == or !=, or of a string concatenation, which calls
    // toString rather than the functional method.
    scanUnlessParameter(tree.getLeftOperand());
    scanUnlessParameter(tree.getRightOperand());
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    if (tree.getMethodSelect() instanceof MemberSelectTree select
        && isParameter(select.getExpression())
        && !PurityChecker.isSameOrOverrides(
            TreeUtils.elementFromUse(tree), parameterFunction, env)) {
      scan(tree.getTypeArguments(), p);
      scan(tree.getArguments(), p);
      return null;
    }
    return super.visitMethodInvocation(tree, p);
  }
}
