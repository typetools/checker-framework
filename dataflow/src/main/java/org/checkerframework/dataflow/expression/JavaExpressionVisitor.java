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
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitArrayAccess(ArrayAccess javaExpr, P p);
    /**
     * Visit an {@link ArrayCreation}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitArrayCreation(ArrayCreation javaExpr, P p);

    /**
     * Visit a {@link BinaryOperation}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitBinaryOperation(BinaryOperation javaExpr, P p);

    /**
     * Visit a {@link ClassName}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitClassName(ClassName javaExpr, P p);

    /**
     * Visit a {@link FieldAccess}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitFieldAccess(FieldAccess javaExpr, P p);

    /**
     * Visit a {@link FormalParameter}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitFormalParameter(FormalParameter javaExpr, P p);

    /**
     * Visit a {@link LocalVariable}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitLocalVariable(LocalVariable javaExpr, P p);

    /**
     * Visit a {@link MethodCall}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitMethodCall(MethodCall javaExpr, P p);

    /**
     * Visit a {@link ThisReference}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitThisReference(ThisReference javaExpr, P p);

    /**
     * Visit an {@link UnaryOperation}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitUnaryOperation(UnaryOperation javaExpr, P p);

    /**
     * Visit an {@link Unknown}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitUnknown(Unknown javaExpr, P p);

    /**
     * Visit a {@link ValueLiteral}.
     *
     * @param javaExpr the JavaExpression to visit
     * @param p the parameter to pass to the visit method
     * @return the result of visiting the {@code javaExpr}
     */
    protected abstract R visitValueLiteral(ValueLiteral javaExpr, P p);
}
