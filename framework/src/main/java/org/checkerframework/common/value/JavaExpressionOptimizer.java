package org.checkerframework.common.value;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionConverter;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/**
 * Optimize the given JavaExpression. If the supplied factory is a {@code
 * ValueAnnotatedTypeFactory}, this implementation replaces any expression that the factory has an
 * exact value for, and does a small (not exhaustive) amount of constant-folding as well. If the
 * factory is some other factory, less optimization occurs.
 *
 * <p>If the supplied factory is {@code ValueAnnotatedTypeFactory}, the its annotations are used
 */
public class JavaExpressionOptimizer extends JavaExpressionConverter {
    private final AnnotatedTypeFactory factory;

    /**
     * Creates a JavaExpressionOptimizer.
     *
     * @param factory an annotated type factory
     */
    public JavaExpressionOptimizer(AnnotatedTypeFactory factory) {
        this.factory = factory;
    }

    @Override
    protected JavaExpression visitLocalVariable(LocalVariable localVarExpr, Void unused) {
        if (!(factory instanceof ValueAnnotatedTypeFactory)) {
            return super.visitLocalVariable(localVarExpr, unused);
        }
        Element element = localVarExpr.getElement();
        Long exactValue =
                ValueCheckerUtils.getExactValue(element, (ValueAnnotatedTypeFactory) factory);
        if (exactValue != null) {
            return new ValueLiteral(localVarExpr.getType(), exactValue.intValue());
        } else {
            return localVarExpr;
        }
    }

    @Override
    protected JavaExpression visitMethodCall(MethodCall methodCallExpr, Void unused) {
        JavaExpression optReceiver = convert(methodCallExpr.getReceiver());
        List<JavaExpression> optArguments = convert(methodCallExpr.getArguments());
        // Length of string literal: convert it to an integer literal.
        if (methodCallExpr.getElement().getSimpleName().contentEquals("length")
                && optReceiver instanceof ValueLiteral) {
            Object value = ((ValueLiteral) optReceiver).getValue();
            if (value instanceof String) {
                return new ValueLiteral(
                        factory.types.getPrimitiveType(TypeKind.INT), ((String) value).length());
            }
        }
        return new MethodCall(
                methodCallExpr.getType(), methodCallExpr.getElement(), optReceiver, optArguments);
    }
}
