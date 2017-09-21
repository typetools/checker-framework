package org.checkerframework.checker.index;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.framework.util.dependenttypes.DependentTypesHelper;
import org.checkerframework.framework.util.dependenttypes.DependentTypesTreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Dependent type helper for array offset expressions. Each array offset expression may be the
 * addition or subtraction of several Java expressions. For example, {@code array.length - 1}.
 */
public class OffsetDependentTypesHelper extends DependentTypesHelper {
    public OffsetDependentTypesHelper(AnnotatedTypeFactory factory) {
        super(factory);
    }

    @Override
    protected String standardizeString(
            final String expression,
            FlowExpressionContext context,
            TreePath localScope,
            boolean useLocalScope) {
        if (DependentTypesError.isExpressionError(expression)) {
            return expression;
        }
        if (indexOf(expression, '-') == -1 && indexOf(expression, '+') == -1) {
            // The expression contains no "-" or "+".
            return super.standardizeString(expression, context, localScope, useLocalScope);
        }

        OffsetEquation equation = OffsetEquation.createOffsetFromJavaExpression(expression);
        if (equation.hasError()) {
            return equation.getError();
        }
        try {
            equation.standardizeAndViewpointAdaptExpressions(context, localScope, useLocalScope);
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            return new DependentTypesError(expression, e).toString();
        }

        return equation.toString();
    }

    @Override
    public TreeAnnotator createDependentTypesTreeAnnotator(AnnotatedTypeFactory factory) {
        return new DependentTypesTreeAnnotator(factory, this) {
            @Override
            public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
                // UpperBoundTreeAnnotator changes the type of array.length to @LTEL("array").
                // If the DependentTypesTreeAnnotator tries to viewpoint-adapt it based on the
                // declaration of length, it will fail.
                if (TreeUtils.isArrayLengthAccess(tree)) {
                    return null;
                }
                return super.visitMemberSelect(tree, type);
            }
        };
    }
}
