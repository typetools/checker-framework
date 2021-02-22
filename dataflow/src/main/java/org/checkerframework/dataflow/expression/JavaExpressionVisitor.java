package org.checkerframework.dataflow.expression;

public abstract class JavaExpressionVisitor<R, P> {

    protected R visit(JavaExpression javaExpr, P p) {
        return javaExpr.accept(this, p);
    }

    protected abstract R visitArrayAccess(ArrayAccess expression, P p);

    protected abstract R visitArrayCreation(ArrayCreation expression, P p);

    protected abstract R visitBinaryOperation(BinaryOperation expression, P p);

    protected abstract R visitClassName(ClassName expression, P p);

    protected abstract R visitFieldAccess(FieldAccess expression, P p);

    protected abstract R visitFormalParameter(FormalParameter expression, P p);

    protected abstract R visitLocalVariable(LocalVariable expression, P p);

    protected abstract R visitMethodCall(MethodCall expression, P p);

    protected abstract R visitThisReference(ThisReference expression, P p);

    protected abstract R visitUnaryOperation(UnaryOperation expression, P p);

    protected abstract R visitUnknown(Unknown expression, P p);

    protected abstract R visitValueLiteral(ValueLiteral expression, P p);
}
