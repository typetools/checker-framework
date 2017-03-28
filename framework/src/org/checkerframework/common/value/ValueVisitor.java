package org.checkerframework.common.value;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.List;
import javax.lang.model.element.Element;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.source.Result;

/**
 * @author plvines
 *     <p>Visitor for the Constant Value type-system
 */
public class ValueVisitor extends BaseTypeVisitor<ValueAnnotatedTypeFactory> {

    public ValueVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * @param exp an integral literal tree
     * @return {@code exp}'s literal value
     */
    private long getIntLiteralValue(LiteralTree exp) {
        Object orgValue = exp.getValue();
        switch (exp.getKind()) {
            case INT_LITERAL:
            case LONG_LITERAL:
                return ((Number) orgValue).longValue();
            case CHAR_LITERAL:
                return (long) ((Character) orgValue);
            default:
                throw new IllegalArgumentException(
                        "exp is not an intergral literal (INT_LITERAL, LONG_LITERAL, CHAR_LITERAL)");
        }
    }

    @Override
    protected ValueAnnotatedTypeFactory createTypeFactory() {
        return new ValueAnnotatedTypeFactory(checker);
    }

    /**
     * Warns about malformed constant-value annotations.
     *
     * <p>Issues an error if any @IntRange annotation has its 'from' value greater than 'to' value.
     *
     * <p>Issues a warning if any constant-value annotation has &gt; MAX_VALUES arguments.
     */
    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        List<? extends ExpressionTree> args = node.getArguments();

        if (args.isEmpty()) {
            // Nothing to do if there are no annotation arguments.
            return super.visitAnnotation(node, p);
        }

        Element element = TreeInfo.symbol((JCTree) node.getAnnotationType());

        if (element.toString().equals(IntRange.class.getName())) {
            // If there are 2 arguments, issue an error if from.greater.than.to.
            // If there are less than 2 arguments, we needn't worry about this problem because the
            // other argument would be defaulted to Long.MIN_VALUE or Long.MAX_VALUE accordingly.
            if (args.size() == 2) {
                ExpressionTree expFrom;
                ExpressionTree expTo;
                AssignmentTree arg0 = (AssignmentTree) args.get(0);
                AssignmentTree arg1 = (AssignmentTree) args.get(1);
                if (arg0.getVariable().toString().equals("from")) {
                    expFrom = arg0.getExpression();
                    expTo = arg1.getExpression();
                } else {
                    expTo = arg0.getExpression();
                    expFrom = arg1.getExpression();
                }

                if (expFrom instanceof LiteralTree && expTo instanceof LiteralTree) {
                    // expression could be a VariableTree but we give up the checks in that case.
                    LiteralTree literalFrom = (LiteralTree) expFrom;
                    LiteralTree literalTo = (LiteralTree) expTo;
                    if (getIntLiteralValue(literalFrom) > getIntLiteralValue(literalTo)) {
                        checker.report(Result.failure("from.greater.than.to"), node);
                        return null;
                    }
                }
            }
        } else if (element.toString().equals(ArrayLen.class.getName())
                || element.toString().equals(BoolVal.class.getName())
                || element.toString().equals(DoubleVal.class.getName())
                || element.toString().equals(IntVal.class.getName())
                || element.toString().equals(StringVal.class.getName())) {
            if (node.getArguments().size() > 0
                    && node.getArguments().get(0).getKind() == Kind.ASSIGNMENT) {
                AssignmentTree argument = (AssignmentTree) node.getArguments().get(0);
                if (argument.getExpression().getKind() == Tree.Kind.NEW_ARRAY) {
                    int numArgs =
                            ((NewArrayTree) argument.getExpression()).getInitializers().size();
                    if (numArgs > ValueAnnotatedTypeFactory.MAX_VALUES) {
                        checker.report(
                                Result.warning(
                                        ((element.toString().equals(IntVal.class.getName()))
                                                ? "too.many.values.given.int"
                                                : "too.many.values.given"),
                                        ValueAnnotatedTypeFactory.MAX_VALUES),
                                node);
                        return null;
                    }
                }
            }
        }

        return super.visitAnnotation(node, p);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        if (node.getExpression().getKind() == Kind.NULL_LITERAL) {
            return null;
        }
        return super.visitTypeCast(node, p);
    }
}
