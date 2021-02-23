package org.checkerframework.dataflow.expression;

import java.util.Map;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class JavaExpressionVPA extends JavaExpressionConverter {
    private final Map<JavaExpression, JavaExpression> mapping;

    public JavaExpressionVPA(Map<JavaExpression, JavaExpression> mapping) {
        this.mapping = mapping;
    }

    @Override
    public JavaExpression convert(JavaExpression javaExpr) {
        JavaExpression newExpr = mapping.get(javaExpr);
        if (newExpr != null) {
            return newExpr;
        }
        return super.convert(javaExpr);
    }
}
