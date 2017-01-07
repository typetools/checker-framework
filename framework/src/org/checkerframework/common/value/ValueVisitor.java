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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
 *     <p>Visitor for the Constant Value type-system.
 */
public class ValueVisitor extends BaseTypeVisitor<ValueAnnotatedTypeFactory> {

    /** helper set that includes all integer literal kinds */
    private final Set<Kind> intLiteralKinds =
            EnumSet.of(Kind.INT_LITERAL, Kind.LONG_LITERAL, Kind.CHAR_LITERAL);

    public ValueVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /** helper function to determine if a given kind is integer literal */
    private boolean isIntLiteral(Kind k) {
        return intLiteralKinds.contains(k);
    }

    /** get value from a give expression tree, assuming the tree kind is int literal */
    private long getIntLiteralValue(ExpressionTree exp) {
        switch (exp.getKind()) {
            case INT_LITERAL:
                return ((Number) ((LiteralTree) exp).getValue()).longValue();
            case LONG_LITERAL:
                return (long) ((LiteralTree) exp).getValue();
            case CHAR_LITERAL:
                return (long) ((Character) ((LiteralTree) exp).getValue());
            default:
                throw new IllegalArgumentException(
                        "exp should be within the covered kinds (INT_LITERAL, LONG_LITERAL, CHAR_LITERAL");
        }
    }

    @Override
    protected ValueAnnotatedTypeFactory createTypeFactory() {
        return new ValueAnnotatedTypeFactory(checker);
    }

    /**
     * Issues an error if any @IntRange annotation has its 'from' value greater than 'to' value.
     * Works together with
     * ValueAnnotatedTypeFactory.ValueTypeAnnotator.replaceWithUnknownValInSpecialCases which treats
     * to value as @UnknownVal in this case.
     *
     * <p>Issues a warning if any constant-value annotation has &gt; MAX_VALUES number of values
     * provided. Works together with
     * ValueAnnotatedTypeFactory.ValueTypeAnnotator.replaceWithUnknownValInSpecialCases which treats
     * the value as @IntRange for @IntVal and @UnknownVal for other *Val annotations in this case.
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
            if (args.size() == 2
                    && args.get(0).getKind() == Kind.ASSIGNMENT
                    && args.get(1).getKind() == Kind.ASSIGNMENT) {
                ExpressionTree expFrom;
                ExpressionTree expTo;

                if (((AssignmentTree) args.get(0)).getVariable().toString().equals("from")) {
                    expFrom = ((AssignmentTree) args.get(0)).getExpression();
                    expTo = ((AssignmentTree) args.get(1)).getExpression();
                } else {
                    expFrom = ((AssignmentTree) args.get(1)).getExpression();
                    expTo = ((AssignmentTree) args.get(0)).getExpression();
                }

                if (isIntLiteral(expFrom.getKind()) && isIntLiteral(expTo.getKind())) {
                    try {
                        long valueFrom = getIntLiteralValue(expFrom);
                        long valueTo = getIntLiteralValue(expTo);
                        if (valueFrom > valueTo) {
                            checker.report(Result.failure("from.greater.than.to"), node);
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace(System.out);
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
