package org.checkerframework.dataflow.expression;

import com.sun.source.tree.Tree;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.UnaryOperationNode;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;

/** JavaExpression for unary operations. */
public class UnaryOperation extends JavaExpression {

  /** The unary operation kind. */
  protected final Tree.Kind operationKind;

  /** The operand. */
  protected final JavaExpression operand;

  /**
   * Create a unary operation.
   *
   * @param type the type of the result
   * @param operationKind the operator
   * @param operand the operand
   */
  public UnaryOperation(TypeMirror type, Tree.Kind operationKind, JavaExpression operand) {
    super(operand.type);
    this.operationKind = operationKind;
    this.operand = operand;
  }

  /**
   * Create a unary operation.
   *
   * @param node the unary operation node
   * @param operand the operand
   */
  public UnaryOperation(UnaryOperationNode node, JavaExpression operand) {
    this(node.getType(), node.getTree().getKind(), operand);
  }

  /**
   * Returns the operator of this unary operation.
   *
   * @return the unary operation kind
   */
  public Tree.Kind getOperationKind() {
    return operationKind;
  }

  /**
   * Returns the operand of this unary operation.
   *
   * @return the operand
   */
  public JavaExpression getOperand() {
    return operand;
  }

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    if (getClass() == clazz) {
      return (T) this;
    }
    return operand.containedOfClass(clazz);
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return operand.isDeterministic(provider);
  }

  @Override
  public boolean isAssignableByOtherCode() {
    return operand.isAssignableByOtherCode();
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return operand.isModifiableByOtherCode();
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof UnaryOperation other)) {
      return false;
    }
    return operationKind == other.getOperationKind() && operand.syntacticEquals(other.operand);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other) || operand.containsSyntacticEqualJavaExpression(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return operand.containsModifiableAliasOf(store, other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationKind, operand);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof UnaryOperation unOp)) {
      return false;
    }
    return operationKind == unOp.getOperationKind() && operand.equals(unOp.operand);
  }

  @Override
  public String toString() {
    String operandString = operand.toString();
    return switch (operationKind) {
      case BITWISE_COMPLEMENT -> "~" + operandString;
      case LOGICAL_COMPLEMENT -> "!" + operandString;
      case POSTFIX_DECREMENT -> operandString + "--";
      case POSTFIX_INCREMENT -> operandString + "++";
      case PREFIX_DECREMENT -> "--" + operandString;
      case PREFIX_INCREMENT -> "++" + operandString;
      case UNARY_MINUS -> "-" + operandString;
      case UNARY_PLUS -> "+" + operandString;
      default -> throw new BugInCF("Unrecognized unary operation kind " + operationKind);
    };
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitUnaryOperation(this, p);
  }
}
