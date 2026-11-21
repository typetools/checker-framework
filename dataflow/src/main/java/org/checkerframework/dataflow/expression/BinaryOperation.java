package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;

/** JavaExpression for binary operations. */
public class BinaryOperation extends JavaExpression {

  /** The binary operation kind. */
  protected final Tree.Kind operationKind;

  /** The left operand. */
  protected final JavaExpression left;

  /** The right operand. */
  protected final JavaExpression right;

  /**
   * Creates a binary operation.
   *
   * @param type the result type
   * @param operationKind the operator
   * @param left the left operand
   * @param right the right operand
   */
  public BinaryOperation(
      TypeMirror type, Tree.Kind operationKind, JavaExpression left, JavaExpression right) {
    super(type);
    this.operationKind = operationKind;
    this.left = left;
    this.right = right;
  }

  /**
   * Creates a binary operation.
   *
   * @param node the binary operation node
   * @param left the left operand
   * @param right the right operand
   */
  public BinaryOperation(BinaryOperationNode node, JavaExpression left, JavaExpression right) {
    this(node.getType(), node.getTree().getKind(), left, right);
  }

  /**
   * Returns the operator of this binary operation.
   *
   * @return the binary operation kind
   */
  public Tree.Kind getOperationKind() {
    return operationKind;
  }

  /**
   * Returns the left operand of this binary operation.
   *
   * @return the left operand
   */
  public JavaExpression getLeft() {
    return left;
  }

  /**
   * Returns the right operand of this binary operation.
   *
   * @return the right operand
   */
  public JavaExpression getRight() {
    return right;
  }

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    if (getClass() == clazz) {
      return (T) this;
    }
    T result = left.containedOfClass(clazz);
    if (result != null) {
      return result;
    }
    return right.containedOfClass(clazz);
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return left.isDeterministic(provider) && right.isDeterministic(provider);
  }

  @Override
  public boolean isAssignableByOtherCode() {
    return left.isAssignableByOtherCode() || right.isAssignableByOtherCode();
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return left.isModifiableByOtherCode() || right.isModifiableByOtherCode();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof BinaryOperation biOp)) {
      return false;
    }
    if (operationKind != biOp.getOperationKind()) {
      return false;
    }
    if (isCommutative()) {
      return (left.equals(biOp.left) && right.equals(biOp.right))
          || (left.equals(biOp.right) && right.equals(biOp.left));
    }
    return left.equals(biOp.left) && right.equals(biOp.right);
  }

  @Override
  public int hashCode() {
    if (isCommutative()) {
      // Use a commutative combination of the operands' hash codes so that equal operands in
      // swapped order (for which `equals()` returns true) hash identically.
      return Objects.hash(operationKind, left.hashCode() + right.hashCode());
    }
    return Objects.hash(operationKind, left, right);
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof BinaryOperation other)) {
      return false;
    }
    return operationKind == other.getOperationKind()
        && left.syntacticEquals(other.left)
        && right.syntacticEquals(other.right);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other)
        || left.containsSyntacticEqualJavaExpression(other)
        || right.containsSyntacticEqualJavaExpression(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return left.containsModifiableAliasOf(store, other)
        || right.containsModifiableAliasOf(store, other);
  }

  /**
   * Returns true if the binary operation is commutative, e.g., x + y == y + x.
   *
   * @return true if the binary operation is commutative
   */
  private boolean isCommutative() {
    return switch (operationKind) {
      case PLUS, MULTIPLY, AND, OR, XOR, EQUAL_TO, NOT_EQUAL_TO, CONDITIONAL_AND, CONDITIONAL_OR ->
          true;
      default -> false;
    };
  }

  @Override
  public String toString() {
    int precedence = precedence(operationKind);
    String leftString = left.toString();
    // Binary operators are left-associative, so a left operand needs parentheses only if its
    // operator binds less tightly, but a right operand also needs them if its operator binds
    // equally tightly:  "a - (b - c)" differs from "a - b - c".
    if (left instanceof BinaryOperation leftOp && precedence(leftOp.operationKind) < precedence) {
      leftString = "(" + leftString + ")";
    }
    String rightString = right.toString();
    if (right instanceof BinaryOperation rightOp
        && precedence(rightOp.operationKind) <= precedence) {
      rightString = "(" + rightString + ")";
    }
    return leftString + " " + operationKindToString(operationKind) + " " + rightString;
  }

  /**
   * Returns the precedence of the given binary operator. A larger number means that the operator
   * binds more tightly.
   *
   * @param operationKind a binary operation kind
   * @return the precedence of the given binary operator
   */
  private static int precedence(Tree.Kind operationKind) {
    return switch (operationKind) {
      case MULTIPLY, DIVIDE, REMAINDER -> 10;
      case PLUS, MINUS -> 9;
      case LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> 8;
      case LESS_THAN, GREATER_THAN, LESS_THAN_EQUAL, GREATER_THAN_EQUAL -> 7;
      case EQUAL_TO, NOT_EQUAL_TO -> 6;
      case AND -> 5;
      case XOR -> 4;
      case OR -> 3;
      case CONDITIONAL_AND -> 2;
      case CONDITIONAL_OR -> 1;
      default -> throw new BugInCF("unhandled " + operationKind);
    };
  }

  /**
   * Returns the Java source code representation of the given operation.
   *
   * @param operationKind a binary operation kind
   * @return the Java source code representation of the given operation
   */
  private String operationKindToString(Tree.Kind operationKind) {
    return switch (operationKind) {
      case CONDITIONAL_AND -> "&&";
      case AND -> "&";
      case OR -> "|";
      case DIVIDE -> "/";
      case EQUAL_TO -> "==";
      case GREATER_THAN -> ">";
      case GREATER_THAN_EQUAL -> ">=";
      case LEFT_SHIFT -> "<<";
      case LESS_THAN -> "<";
      case LESS_THAN_EQUAL -> "<=";
      case MINUS -> "-";
      case MULTIPLY -> "*";
      case NOT_EQUAL_TO -> "!=";
      case CONDITIONAL_OR -> "||";
      case PLUS -> "+";
      case REMAINDER -> "%";
      case RIGHT_SHIFT -> ">>";
      case UNSIGNED_RIGHT_SHIFT -> ">>>";
      case XOR -> "^";
      default -> throw new BugInCF("unhandled " + operationKind);
    };
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitBinaryOperation(this, p);
  }
}
