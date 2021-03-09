package org.checkerframework.dataflow.expression;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class has methods to viewpoint-adapt {@link JavaExpression} by replacing {@link
 * ThisReference} and {@link FormalParameter} expressions with the given {@link JavaExpression}s.
 */
public class ViewpointAdaptJavaExpression extends JavaExpressionConverter {

    // Public static methods

    /**
     * Replace {@link FormalParameter}s by {@code args} in {@code javaExpr}. ({@link ThisReference}s
     * are not converted.)
     *
     * @param javaExpr the expression to viewpoint-adapt
     * @param args the expressions that replace {@link FormalParameter}s; if null, {@link
     *     FormalParameter}s are not replaced
     * @return the viewpoint-adapted expression
     */
    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr, @Nullable List<JavaExpression> args) {
        return viewpointAdapt(javaExpr, null, args);
    }
    /**
     * Replace {@link ThisReference} with {@code thisReference} in {@code javaExpr}. ({@link
     * FormalParameter} are not replaced.
     *
     * @param javaExpr the expression to viewpoint-adapt
     * @param thisReference the expression that replaces occurrences of {@link ThisReference}; if
     *     null, {@link ThisReference}s are not replaced
     * @return the viewpoint-adapted expression
     */
    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr, @Nullable JavaExpression thisReference) {
        return viewpointAdapt(javaExpr, thisReference, null);
    }

    /**
     * Replace {@link FormalParameter}s with {@code args} and {@link ThisReference} with {@code
     * thisReference} in {@code javaExpr}.
     *
     * @param javaExpr the expression to viewpoint-adapt
     * @param thisReference the expression that replaces occurrences of {@link ThisReference}; if
     *     null, {@link ThisReference}s are not replaced
     * @param args the expressions that replaces {@link FormalParameter}s; if null, {@link
     *     FormalParameter}s are not replaced
     * @return the viewpoint-adapted expression
     */
    public static JavaExpression viewpointAdapt(
            JavaExpression javaExpr,
            @Nullable JavaExpression thisReference,
            @Nullable List<JavaExpression> args) {
        return new ViewpointAdaptJavaExpression(thisReference, args).convert(javaExpr);
    }

    // Fields

    /** List of arguments used to replace occurrences {@link FormalParameter}s. */
    private final @Nullable List<JavaExpression> args;

    /** The expression to replace occurrences of {@link ThisReference}s. */
    private final @Nullable JavaExpression thisReference;

    // Instance methods

    /**
     * Creates a {@link JavaExpressionConverter} that viewpoint-adapts using the given {@code
     * thisReference} and {@code args}.
     *
     * @param thisReference the expression that replaces occurrences of {@link ThisReference};
     *     {@code null} means don't replace
     * @param args list of arguments that replaces occurrences {@link FormalParameter}s; {@code
     *     null} means don't replace
     */
    private ViewpointAdaptJavaExpression(
            @Nullable JavaExpression thisReference, @Nullable List<JavaExpression> args) {
        this.args = args;
        this.thisReference = thisReference;
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
            if (index < args.size()) {
                return args.get(index);
            }
        }
        return super.visitFormalParameter(parameterExpr, unused);
    }
}
