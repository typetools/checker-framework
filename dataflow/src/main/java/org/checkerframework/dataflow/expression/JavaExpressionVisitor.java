package org.checkerframework.dataflow.expression;

/**
 * A simple visitor for {@link JavaExpression}.
 *
 * @param <R> the return type of the visit methods
 * @param <P> the parameter passed to the visit methods
 */
public abstract class JavaExpressionVisitor<R, P> {

    /**
     * Visits the given {@code javaExpr}.
     *
     * @param javaExpr the expression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the expression
     */
    public R visit(JavaExpression javaExpr, P p) {
        return javaExpr.accept(this, p);
    }

    /**
     * Visit an {@link ArrayAccess}.
     *
     * @param arrayAccessExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code arrayAccessExpr}
     */
    protected abstract R visitArrayAccess(ArrayAccess arrayAccessExpr, P p);
    /**
     * Visit an {@link ArrayCreation}.
     *
     * @param arrayCreationExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code arrayCreationExpr}
     */
    protected abstract R visitArrayCreation(ArrayCreation arrayCreationExpr, P p);

    /**
     * Visit a {@link BinaryOperation}.
     *
     * @param binaryOpExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code binaryOpExpr}
     */
    protected abstract R visitBinaryOperation(BinaryOperation binaryOpExpr, P p);

    /**
     * Visit a {@link ClassName}.
     *
     * @param classNameExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code classNameExpr}
     */
    protected abstract R visitClassName(ClassName classNameExpr, P p);

    /**
     * Visit a {@link FieldAccess}.
     *
     * @param fieldAccessExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code fieldAccessExpr}
     */
    protected abstract R visitFieldAccess(FieldAccess fieldAccessExpr, P p);

    /**
     * Visit a {@link LocalVariable}.
     *
     * @param localVarExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code localVarExpr}
     */
    protected abstract R visitLocalVariable(LocalVariable localVarExpr, P p);

    /**
     * Visit a {@link MethodCall}.
     *
     * @param methodCallExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code methodCallExpr}
     */
    protected abstract R visitMethodCall(MethodCall methodCallExpr, P p);

    /**
     * Visit a {@link ThisReference}.
     *
     * @param thisExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code thisExpr}
     */
    protected abstract R visitThisReference(ThisReference thisExpr, P p);

    /**
     * Visit an {@link UnaryOperation}.
     *
     * @param unaryOpExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code unaryOpExpr}
     */
    protected abstract R visitUnaryOperation(UnaryOperation unaryOpExpr, P p);

    /**
     * Visit an {@link Unknown}.
     *
     * @param unknownExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code unknownExpr}
     */
    protected abstract R visitUnknown(Unknown unknownExpr, P p);

    /**
     * Visit a {@link ValueLiteral}.
     *
     * @param literalExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code literalExpr}
     */
    protected abstract R visitValueLiteral(ValueLiteral literalExpr, P p);
}
