package org.checkerframework.dataflow.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class JavaExpressionVPA extends JavaExpressionVisitor<JavaExpression, Void> {
    private final Map<JavaExpression, JavaExpression> mapping;

    public JavaExpressionVPA(Map<JavaExpression, JavaExpression> mapping) {
        this.mapping = mapping;
    }

    @Nullable JavaExpression defaultAction(JavaExpression expression) {
        return mapping.get(expression);
    }

    public List<@PolyNull JavaExpression> visit(List<@PolyNull JavaExpression> list) {
        List<@PolyNull JavaExpression> newList = new ArrayList<>();
        for (JavaExpression expression : list) {
            if (expression == null) {
                newList.add(null);
            } else {
                newList.add(visit(expression));
            }
        }
        return newList;
    }

    public JavaExpression visit(JavaExpression expression) {
        return expression.accept(this, null);
    }

    @Override
    protected JavaExpression visitArrayAccess(ArrayAccess arrayAccessExpr, Void unused) {
        JavaExpression vpa = defaultAction(arrayAccessExpr);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayAccess(
                arrayAccessExpr.type, visit(arrayAccessExpr.array), visit(arrayAccessExpr.index));
    }

    @Override
    protected JavaExpression visitArrayCreation(ArrayCreation arrayCreationExpr, Void unused) {
        JavaExpression vpa = defaultAction(arrayCreationExpr);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayCreation(
                arrayCreationExpr.getType(),
                visit(arrayCreationExpr.dimensions),
                visit(arrayCreationExpr.initializers));
    }

    @Override
    protected JavaExpression visitBinaryOperation(BinaryOperation binaryOpExpr, Void unused) {
        JavaExpression vpa = defaultAction(binaryOpExpr);
        if (vpa != null) {
            return vpa;
        }
        return new BinaryOperation(
                binaryOpExpr.getType(),
                binaryOpExpr.operationKind,
                visit(binaryOpExpr.left),
                visit(binaryOpExpr.right));
    }

    @Override
    protected JavaExpression visitClassName(ClassName classNameExpr, Void unused) {
        JavaExpression vpa = defaultAction(classNameExpr);
        if (vpa != null) {
            return vpa;
        }
        return classNameExpr;
    }

    @Override
    protected JavaExpression visitFieldAccess(FieldAccess fieldAccessExpr, Void unused) {
        JavaExpression vpa = defaultAction(fieldAccessExpr);
        if (vpa != null) {
            return vpa;
        }
        return new FieldAccess(
                visit(fieldAccessExpr.receiver),
                fieldAccessExpr.getType(),
                fieldAccessExpr.getField());
    }

    @Override
    protected JavaExpression visitFormalParameter(FormalParameter parameterExpr, Void unused) {
        JavaExpression vpa = defaultAction(parameterExpr);
        if (vpa != null) {
            return vpa;
        }
        return parameterExpr;
    }

    @Override
    protected JavaExpression visitLocalVariable(LocalVariable localVarExpr, Void unused) {
        JavaExpression vpa = defaultAction(localVarExpr);
        if (vpa != null) {
            return vpa;
        }
        return localVarExpr;
    }

    @Override
    protected JavaExpression visitMethodCall(MethodCall methodCallExpr, Void unused) {
        JavaExpression vpa = defaultAction(methodCallExpr);
        if (vpa != null) {
            return vpa;
        }
        return new MethodCall(
                methodCallExpr.getType(),
                methodCallExpr.getElement(),
                visit(methodCallExpr.getReceiver()),
                visit(methodCallExpr.getArguments()));
    }

    @Override
    protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
        JavaExpression vpa = defaultAction(thisExpr);
        if (vpa != null) {
            return vpa;
        }
        return thisExpr;
    }

    @Override
    protected JavaExpression visitUnaryOperation(UnaryOperation unaryOpExpr, Void unused) {
        JavaExpression vpa = defaultAction(unaryOpExpr);
        if (vpa != null) {
            return vpa;
        }
        return new UnaryOperation(
                unaryOpExpr.getType(),
                unaryOpExpr.getOperationKind(),
                visit(unaryOpExpr.getOperand()));
    }

    @Override
    protected JavaExpression visitUnknown(Unknown unknownExpr, Void unused) {
        JavaExpression vpa = defaultAction(unknownExpr);
        if (vpa != null) {
            return vpa;
        }
        return unknownExpr;
    }

    @Override
    protected JavaExpression visitValueLiteral(ValueLiteral literalExpr, Void unused) {
        JavaExpression vpa = defaultAction(literalExpr);
        if (vpa != null) {
            return vpa;
        }
        return literalExpr;
    }
}
