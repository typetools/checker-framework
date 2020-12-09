package org.checkerframework.checker.index;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionContext;
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
            JavaExpressionContext context,
            TreePath localScope,
            boolean useLocalScope) {
        if (DependentTypesError.isExpressionError(expression)) {
            return expression;
        }
        if (expression.indexOf('-') == -1 && expression.indexOf('+') == -1) {
            // The expression contains no "-" or "+", so it can be standardized directly.
            JavaExpression result;
            try {
                result =
                        JavaExpressionParseUtil.parse(
                                expression, context, localScope, useLocalScope);
            } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
                return new DependentTypesError(expression, e).toString();
            }
            if (result == null) {
                return new DependentTypesError(expression, " ").toString();
            }
            if (result instanceof FieldAccess && ((FieldAccess) result).isFinal()) {
                Object constant = ((FieldAccess) result).getField().getConstantValue();
                if (constant != null && !(constant instanceof String)) {
                    return constant.toString();
                }
            }
            return result.toString();
        }

        // The expression is a sum of several terms. This expression is standardized by splitting it
        // into individual terms in an OffsetEquation and standardizing each term.
        OffsetEquation equation = OffsetEquation.createOffsetFromJavaExpression(expression);
        if (equation.hasError()) {
            return equation.getError();
        }
        try {
            // Standardize individual terms of the expression.
            equation.standardizeAndViewpointAdaptExpressions(
                    context, localScope, useLocalScope, factory);
        } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
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
