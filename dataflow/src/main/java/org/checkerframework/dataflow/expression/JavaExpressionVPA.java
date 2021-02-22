package org.checkerframework.dataflow.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.javacutil.BugInCF;

@AnnotatedFor("nullness")
public class JavaExpressionVPA {
    private final Map<JavaExpression, JavaExpression> mapping;

    public JavaExpressionVPA(Map<JavaExpression, JavaExpression> mapping) {
        this.mapping = mapping;
    }

    @Nullable JavaExpression defaultAction(JavaExpression expression) {
        return mapping.get(expression);
    }

    JavaExpression visit(JavaExpression expression) {
        if (expression == null) {
            throw new BugInCF("JavaExpression null.");
        }
        throw new BugInCF(
                "Unexpected JavaExpression: %s class: %s", expression, expression.getClass());
    }

    List<@PolyNull JavaExpression> visit(List<@PolyNull JavaExpression> list) {
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

    JavaExpression visit(ArrayAccess expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayAccess(expression.type, visit(expression.array), visit(expression.index));
    }

    JavaExpression visit(ArrayCreation expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new ArrayCreation(
                expression.getType(), visit(expression.dimensions), visit(expression.initializers));
    }

    JavaExpression visit(BinaryOperation expression) {
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

    JavaExpression visit(ClassName expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    JavaExpression visit(FieldAccess expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new FieldAccess(
                visit(expression.receiver), expression.getType(), expression.getField());
    }

    JavaExpression visit(FormalParameter expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    JavaExpression visit(LocalVariable expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    JavaExpression visit(MethodCall expression) {
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

    JavaExpression visit(ThisReference expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    JavaExpression visit(UnaryOperation expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return new UnaryOperation(
                expression.getType(),
                expression.getOperationKind(),
                visit(expression.getOperand()));
    }

    JavaExpression visit(Unknown expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }

    JavaExpression visit(ValueLiteral expression) {
        JavaExpression vpa = defaultAction(expression);
        if (vpa != null) {
            return vpa;
        }
        return expression;
    }
}
