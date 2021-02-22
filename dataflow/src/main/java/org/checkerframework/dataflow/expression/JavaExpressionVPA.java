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
    protected JavaExpression visitArrayAccess(ArrayAccess expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayAccess(expression.type, visit(expression.array), visit(expression.index));
    }

    @Override
    protected JavaExpression visitArrayCreation(ArrayCreation expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayCreation(
                expression.getType(), visit(expression.dimensions), visit(expression.initializers));
    }

    @Override
    protected JavaExpression visitBinaryOperation(BinaryOperation expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new BinaryOperation(
                expression.getType(),
                expression.operationKind,
                visit(expression.left),
                visit(expression.right));
    }

    @Override
    protected JavaExpression visitClassName(ClassName expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    @Override
    protected JavaExpression visitFieldAccess(FieldAccess expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new FieldAccess(
                visit(expression.receiver), expression.getType(), expression.getField());
    }

    @Override
    protected JavaExpression visitFormalParameter(FormalParameter expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    @Override
    protected JavaExpression visitLocalVariable(LocalVariable expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    @Override
    protected JavaExpression visitMethodCall(MethodCall expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new MethodCall(
                expression.getType(),
                expression.getElement(),
                visit(expression.getReceiver()),
                visit(expression.getArguments()));
    }

    @Override
    protected JavaExpression visitThisReference(ThisReference expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    @Override
    protected JavaExpression visitUnaryOperation(UnaryOperation expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new UnaryOperation(
                expression.getType(),
                expression.getOperationKind(),
                visit(expression.getOperand()));
    }

    @Override
    protected JavaExpression visitUnknown(Unknown expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    @Override
    protected JavaExpression visitValueLiteral(ValueLiteral expression, Void unused) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }
}
