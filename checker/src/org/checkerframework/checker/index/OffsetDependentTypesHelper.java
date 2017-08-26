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
        if (indexOf(expression, '-', '+', 0) == -1) {
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

    /**
     * Returns the index of the first occurrence of one of two characters in an expression, starting
     * from a specified index. If there is no such occurrence, returns -1.
     *
     * @param expression the expression being searched
     * @param a the first character searched for
     * @param b the second character searched for
     * @param index the starting index of the search
     * @return the index of the first occurrence of {@code a} or {@code b} in {@code expression}
     *     starting from {@code index}, or -1
     */
    private int indexOf(String expression, char a, char b, int index) {
        int aIndex = expression.indexOf(a, index);
        int bIndex = expression.indexOf(b, index);
        if (aIndex == -1) {
            return bIndex;
        } else if (bIndex == -1) {
            return aIndex;
        } else {
            return Math.min(aIndex, bIndex);
        }
    }

    @Override
    public TreeAnnotator createDependentTypesTreeAnnotator(AnnotatedTypeFactory factory) {
        return new DependentTypesTreeAnnotator(factory, this) {
            @Override
            public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
                // UpperBoundTreeAnnotator changes the type of array.length to @LTEL
                // ("array"). If the DependentTypesTreeAnnotator tries to viewpoint
                // adapt it based on the declaration of length; it will fail.
                if (TreeUtils.isArrayLengthAccess(tree)) {
                    return null;
                }
                return super.visitMemberSelect(tree, type);
            }
        };
    }
}
