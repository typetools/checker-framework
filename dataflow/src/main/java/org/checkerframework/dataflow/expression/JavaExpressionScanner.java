package org.checkerframework.dataflow.expression;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A simple scanner for {@link JavaExpression}.
 *
 * @param <P> the parameter passed to the scan methods
 */
public abstract class JavaExpressionScanner<P> extends JavaExpressionVisitor<Void, P> {

  /**
   * Scans the JavaExpression.
   *
   * @param javaExpression the expression to scan.
   * @param p parameter to pass
   */
  public void scan(JavaExpression javaExpression, P p) {
    visit(javaExpression, p);
  }

  /**
   * Scans each JavaExpression in {@code expressions}.
   *
   * @param expressions a list of JavaExpressions to scan
   * @param p pameter to pass
   */
  public void scan(List<? extends @Nullable JavaExpression> expressions, P p) {
    for (JavaExpression expression : expressions) {
      if (expression != null) {
        visit(expression, p);
      }
    }
  }

  @Override
  protected Void visitArrayAccess(ArrayAccess arrayAccessExpr, P p) {
    visit(arrayAccessExpr.getArray(), p);
    visit(arrayAccessExpr.getIndex(), p);
    return null;
  }

  @Override
  protected Void visitArrayCreation(ArrayCreation arrayCreationExpr, P p) {
    scan(arrayCreationExpr.getDimensions(), p);
    scan(arrayCreationExpr.getInitializers(), p);
    return null;
  }

  @Override
  protected Void visitBinaryOperation(BinaryOperation binaryOpExpr, P p) {
    visit(binaryOpExpr.getLeft(), p);
    visit(binaryOpExpr.getRight(), p);
    return null;
  }

  @Override
  protected Void visitClassName(ClassName classNameExpr, P p) {
    return null;
  }

  @Override
  protected Void visitFormalParameter(FormalParameter parameterExpr, P p) {
    return null;
  }

  @Override
  protected Void visitFieldAccess(FieldAccess fieldAccessExpr, P p) {
    visit(fieldAccessExpr.getReceiver(), p);
    return null;
  }

  @Override
  protected Void visitLocalVariable(LocalVariable localVarExpr, P p) {
    return null;
  }

  @Override
  protected Void visitMethodCall(MethodCall methodCallExpr, P p) {
    visit(methodCallExpr.getReceiver(), p);
    scan(methodCallExpr.getArguments(), p);
    return null;
  }

  @Override
  protected Void visitThisReference(ThisReference thisExpr, P p) {
    return null;
  }

  @Override
  protected Void visitUnaryOperation(UnaryOperation unaryOpExpr, P p) {
    visit(unaryOpExpr.getOperand(), p);
    return null;
  }

  @Override
  protected Void visitUnknown(Unknown unknownExpr, P p) {
    return null;
  }

  @Override
  protected Void visitValueLiteral(ValueLiteral literalExpr, P p) {
    return null;
  }
}
