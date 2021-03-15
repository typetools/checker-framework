package org.checkerframework.checker.index;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.ValueCheckerUtils;
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
            @Nullable TreePath localVarPath) {
        if (DependentTypesError.isExpressionError(expression)) {
            return expression;
        }
        JavaExpression result;
        try {
            result = JavaExpressionParseUtil.parse(expression, context, localVarPath);
        } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
            return new DependentTypesError(expression, e).toString();
        }
        if (result == null) {
            return new DependentTypesError(expression, /*error message=*/ " ").toString();
        }

        // TODO: Maybe move this into the superclass standardizeString, then remove this class.
        result = ValueCheckerUtils.optimize(result, factory);

        return result.toString();
    }

    @Override
    public TreeAnnotator createDependentTypesTreeAnnotator() {
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
