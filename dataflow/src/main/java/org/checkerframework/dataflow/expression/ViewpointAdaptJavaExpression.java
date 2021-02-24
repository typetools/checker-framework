package org.checkerframework.dataflow.expression;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ViewpointAdaptJavaExpression extends JavaExpressionConverter {
    private final @Nullable List<JavaExpression> args;
    private final @Nullable JavaExpression thisReference;

    private ViewpointAdaptJavaExpression(
            @Nullable JavaExpression thisReference, @Nullable List<JavaExpression> args) {
        this.args = args;
        this.thisReference = thisReference;
    }

    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr, @Nullable List<JavaExpression> args) {
        return viewpointAdapt(javaExpr, null, args);
    }

    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr, @Nullable JavaExpression thisReference) {
        return viewpointAdapt(javaExpr, thisReference, null);
    }

    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr,
            @Nullable JavaExpression thisReference,
            @Nullable List<JavaExpression> args) {
        return new ViewpointAdaptJavaExpression(thisReference, args).convert(javaExpr);
    }

    @Override
    protected JavaExpression visitThisReference(ThisReference thisExpr, Void unused) {
        if (thisReference != null) {
            return thisReference;
        }
        return super.visitThisReference(thisExpr, unused);
    }

    @Override
    protected JavaExpression visitFormalParameter(FormalParameter parameterExpr, Void unused) {
        if (args != null) {
            int index = parameterExpr.getIndex() - 1;
            // TODO: throw viewpoint-adaption exception if index is out of bounds?
            return args.get(index);
        }
        return super.visitFormalParameter(parameterExpr, unused);
    }
}
