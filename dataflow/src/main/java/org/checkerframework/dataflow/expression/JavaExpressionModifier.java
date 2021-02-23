package org.checkerframework.dataflow.expression;

public abstract class JavaExpressionModifier extends JavaExpressionVisitor<JavaExpression, Void> {

    public JavaExpression visit(JavaExpression javaExpr) {
        return super.visit(javaExpr, null);
    }

    @Override
    protected JavaExpression visitArrayAccess(ArrayAccess arrayAccessExpr, Void unused) {
        JavaExpression array = visit(arrayAccessExpr.getArray());
        JavaExpression index = visit(arrayAccessExpr.getIndex());
        return null;
    }

    @Override
    protected JavaExpression visitArrayCreation(ArrayCreation arrayCreationExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitBinaryOperation(BinaryOperation binaryOpExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitClassName(ClassName classNameExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitFieldAccess(FieldAccess fieldAccessExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitFormalParameter(FormalParameter parameterExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitLocalVariable(LocalVariable localVarExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitMethodCall(MethodCall methodCallExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitUnaryOperation(UnaryOperation unaryOpExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitUnknown(Unknown unknownExpr, Void unused) {
        return null;
    }

    @Override
    protected JavaExpression visitValueLiteral(ValueLiteral literalExpr, Void unused) {
        return null;
    }
}
