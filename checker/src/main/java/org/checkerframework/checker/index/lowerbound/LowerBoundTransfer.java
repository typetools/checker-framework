package org.checkerframework.checker.index.lowerbound;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LowerBoundUnknown;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Implements dataflow refinement rules based on tests: &lt;, &gt;, ==, and their derivatives.
 *
 * <p>Also implements the logic for binary operations: +, -, *, /, and %.
 *
 * <p>&gt;, &lt;, &ge;, &le;, ==, and != nodes are represented as combinations of &gt; and &ge;
 * (e.g. == is &ge; in both directions in the then branch), and implement refinements based on these
 * decompositions.
 *
 * <pre>
 * Refinement/transfer rules for conditionals:
 *
 * There are two "primitives":
 *
 * x &gt; y, which implies things about x based on y's type:
 *
 * y has type:    implies x has type:
 *  gte-1                nn
 *  nn                   pos
 *  pos                  pos
 *
 * and x &ge; y:
 *
 * y has type:    implies x has type:
 *  gte-1                gte-1
 *  nn                   nn
 *  pos                  pos
 *
 * These two "building blocks" can be combined to make all
 * other conditional expressions:
 *
 * EXPR             THEN          ELSE
 * x &gt; y            x &gt; y         y &ge; x
 * x &ge; y           x &ge; y        y &gt; x
 * x &lt; y            y &gt; x         x &ge; y
 * x &le; y           y &ge; x        x &gt; y
 *
 * Or, more formally:
 *
 * EXPR        THEN                                        ELSE
 * x &gt; y       x_refined = GLB(x_orig, promote(y))         y_refined = GLB(y_orig, x)
 * x &ge; y      x_refined = GLB(x_orig, y)                  y_refined = GLB(y_orig, promote(x))
 * x &lt; y       y_refined = GLB(y_orig, promote(x))         x_refined = GLB(x_orig, y)
 * x &le; y      y_refined = GLB(y_orig, x)                  x_refined = GLB(x_orig, promote(y))
 *
 * where GLB is the greatest lower bound and promote is the increment
 * function on types (or, equivalently, the function specified by the "x
 * &gt; y" information above).
 *
 * There's also ==, which is a special case. Only the THEN
 * branch is refined:
 *
 * EXPR             THEN                   ELSE
 * x == y           x &ge; y &amp;&amp; y &ge; x       nothing known
 *
 * or, more formally:
 *
 * EXPR            THEN                                    ELSE
 * x == y          x_refined = GLB(x_orig, y_orig)         nothing known
 *                y_refined = GLB(x_orig, y_orig)
 *
 * finally, not equal:
 *
 * EXPR             THEN                   ELSE
 * x != y           nothing known          x &ge; y &amp;&amp; y &ge; x
 *
 * more formally:
 *
 * EXPR            THEN               ELSE
 * x != y          nothing known      x_refined = GLB(x_orig, y_orig)
 *                                   y_refined = GLB(x_orig, y_orig)
 *
 * </pre>
 *
 * Dividing these rules up by cases, this class implements:
 *
 * <ul>
 *   <li>1. The rule described above for &gt;
 *   <li>2. The rule described above for &ge;
 *   <li>3. The rule described above for &lt;
 *   <li>4. The rule described above for &le;
 *   <li>5. The rule described above for ==
 *   <li>6. The rule described above for !=
 *   <li>7. A special refinement for != when the value being compared to is a compile-time constant
 *       with a value exactly equal to -1 or 0 (i.e. {@code x != -1} and x is GTEN1 implies x is
 *       non-negative). Maybe two rules?
 *   <li>8. When a compile-time constant -2 is added to a positive, the result is GTEN1
 *   <li>9. When a compile-time constant 2 is added to a GTEN1, the result is positive
 *   <li>10. When a positive is added to a positive, the result is positive
 *   <li>11. When a non-negative is added to any other type, the result is that other type
 *   <li>12. When a GTEN1 is added to a positive, the result is non-negative
 *   <li>13. When the left side of a subtraction expression is &gt; the right side according to the
 *       LessThanChecker, the result of the subtraction expression is positive
 *   <li>14. When the left side of a subtraction expression is &ge; the right side according to the
 *       LessThanChecker, the result of the subtraction expression is non-negative
 *   <li>15. special handling for when the left side is the length of an array or String that's
 *       stored as a field, and the right side is a compile time constant. Do we need this?
 *   <li>16. Multiplying any value by a compile time constant of 1 preserves its type
 *   <li>17. Multiplying two positives produces a positive
 *   <li>18. Multiplying a positive and a non-negative produces a non-negative
 *   <li>19. Multiplying two non-negatives produces a non-negative
 *   <li>20. When the result of Math.random is multiplied by an array length, the result is
 *       NonNegative.
 *   <li>21. literal 0 divided by anything is non-negative
 *   <li>22. anything divided by literal zero is bottom
 *   <li>23. literal 1 divided by a positive or non-negative is non-negative
 *   <li>24. literal 1 divided by anything else is GTEN1
 *   <li>25. anything divided by literal 1 is itself
 *   <li>26. a positive or non-negative divided by a positive or non-negative is non-negative
 *   <li>27. anything modded by literal 1 or -1 is non-negative
 *   <li>28. a positive or non-negative modded by anything is non-negative
 *   <li>29. a GTEN1 modded by anything is GTEN1
 *   <li>30. anything right-shifted by a non-negative is non-negative
 *   <li>31. anything bitwise-anded by a non-negative is non-negative
 *   <li>32. If a and b are non-negative and {@code a <= b} and {@code a != b}, then b is pos.
 *   <li>33. A char is always non-negative
 * </ul>
 */
public class LowerBoundTransfer extends IndexAbstractTransfer {

    /** The canonical {@link GTENegativeOne} annotation. */
    public final AnnotationMirror GTEN1;
    /** The canonical {@link NonNegative} annotation. */
    public final AnnotationMirror NN;
    /** The canonical {@link Positive} annotation. */
    public final AnnotationMirror POS;
    /** The canonical {@link LowerBoundUnknown} annotation. */
    public final AnnotationMirror UNKNOWN;

    // The ATF (Annotated Type Factory).
    private LowerBoundAnnotatedTypeFactory aTypeFactory;

    public LowerBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        aTypeFactory = (LowerBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        // Initialize qualifiers.
        GTEN1 = aTypeFactory.GTEN1;
        NN = aTypeFactory.NN;
        POS = aTypeFactory.POS;
        UNKNOWN = aTypeFactory.UNKNOWN;
    }

    /**
     * Refines GTEN1 to NN if it is not equal to -1, and NN to Pos if it is not equal to 0.
     * Implements case 7.
     *
     * @param mLiteral a potential literal
     * @param otherNode the node on the other side of the ==/!=
     * @param otherAnno the annotation of the other side of the ==/!=
     */
    private void notEqualToValue(
            Node mLiteral, Node otherNode, AnnotationMirror otherAnno, CFStore store) {

        Long integerLiteral =
                ValueCheckerUtils.getExactValue(
                        mLiteral.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());

        if (integerLiteral == null) {
            return;
        }
        long intLiteral = integerLiteral.longValue();

        if (intLiteral == 0) {
            if (aTypeFactory.areSameByClass(otherAnno, NonNegative.class)) {
                List<Node> internals = splitAssignments(otherNode);
                for (Node internal : internals) {
                    JavaExpression je = JavaExpression.fromNode(internal);
                    store.insertValue(je, POS);
                }
            }
        } else if (intLiteral == -1) {
            if (aTypeFactory.areSameByClass(otherAnno, GTENegativeOne.class)) {
                List<Node> internals = splitAssignments(otherNode);
                for (Node internal : internals) {
                    JavaExpression je = JavaExpression.fromNode(internal);
                    store.insertValue(je, NN);
                }
            }
        }
    }

    /**
     * Implements the transfer rules for both equal nodes and not-equals nodes (i.e. cases 5, 6,
     * 32).
     */
    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> result,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        result =
                super.strengthenAnnotationOfEqualTo(
                        result, firstNode, secondNode, firstValue, secondValue, notEqualTo);

        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, secondNode, firstNode);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        // There is also special processing to look
        // for literals on one side of the equals and a GTEN1 or NN on the other, so that
        // those types can be promoted in the branch where their values are not equal to certain
        // literals.
        CFStore notEqualsStore = notEqualTo ? rfi.thenStore : rfi.elseStore;
        notEqualToValue(rfi.left, rfi.right, rfi.rightAnno, notEqualsStore);
        notEqualToValue(rfi.right, rfi.left, rfi.leftAnno, notEqualsStore);

        notEqualsLessThan(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, notEqualsStore);
        notEqualsLessThan(rfi.right, rfi.rightAnno, rfi.left, rfi.leftAnno, notEqualsStore);

        return rfi.newResult;
    }

    /** Implements case 32. */
    private void notEqualsLessThan(
            Node leftNode,
            AnnotationMirror leftAnno,
            Node otherNode,
            AnnotationMirror otherAnno,
            CFStore store) {
        if (!isNonNegative(leftAnno) || !isNonNegative(otherAnno)) {
            return;
        }
        JavaExpression otherJe = JavaExpression.fromNode(otherNode);
        if (aTypeFactory
                .getLessThanAnnotatedTypeFactory()
                .isLessThanOrEqual(leftNode.getTree(), otherJe.toString())) {
            store.insertValue(otherJe, POS);
        }
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. Changes the type of left (the
     * greater one) to one closer to bottom than the type of right. Can't call the promote function
     * from the ATF directly because a new expression isn't introduced here - the modifications have
     * to be made to an existing one.
     *
     * <p>This implements parts of cases 1, 2, 3, and 4 using the decomposition strategy described
     * in the Javadoc of this class.
     */
    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {

        if (rightAnno == null || leftAnno == null) {
            return;
        }

        JavaExpression leftJe = JavaExpression.fromNode(left);

        if (AnnotationUtils.areSame(rightAnno, GTEN1)) {
            store.insertValue(leftJe, NN);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, NN)) {
            store.insertValue(leftJe, POS);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, POS)) {
            store.insertValue(leftJe, POS);
            return;
        }
    }

    /**
     * Refines left to exactly the level of right, since in the worst case they're equal. Modifies
     * an existing type in the store, but has to be careful not to overwrite a more precise existing
     * type.
     *
     * <p>This implements parts of cases 1, 2, 3, and 4 using the decomposition strategy described
     * in this class's Javadoc.
     */
    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {

        if (rightAnno == null || leftAnno == null) {
            return;
        }

        JavaExpression leftJe = JavaExpression.fromNode(left);

        AnnotationMirror newLBType =
                aTypeFactory.getQualifierHierarchy().greatestLowerBound(rightAnno, leftAnno);

        store.insertValue(leftJe, newLBType);
    }

    /**
     * Returns an annotation mirror representing the result of subtracting one from {@code oldAnm}.
     */
    private AnnotationMirror anmAfterSubtractingOne(AnnotationMirror oldAnm) {
        if (isPositive(oldAnm)) {
            return NN;
        } else if (isNonNegative(oldAnm)) {
            return GTEN1;
        } else {
            return UNKNOWN;
        }
    }

    /** Returns an annotation mirror representing the result of adding one to {@code oldAnm}. */
    private AnnotationMirror anmAfterAddingOne(AnnotationMirror oldAnm) {
        if (isNonNegative(oldAnm)) {
            return POS;
        } else if (isGTEN1(oldAnm)) {
            return NN;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Helper method for getAnnotationForPlus. Handles addition of constants (cases 8 and 9).
     *
     * @param val the integer value of the constant
     * @param nonLiteralType the type of the side of the expression that isn't a constant
     */
    private AnnotationMirror getAnnotationForLiteralPlus(int val, AnnotationMirror nonLiteralType) {
        if (val == -2) {
            if (isPositive(nonLiteralType)) {
                return GTEN1;
            }
        } else if (val == -1) {
            return anmAfterSubtractingOne(nonLiteralType);
        } else if (val == 0) {
            return nonLiteralType;
        } else if (val == 1) {
            return anmAfterAddingOne(nonLiteralType);
        } else if (val >= 2) {
            if (isGTEN1(nonLiteralType)) {
                // 2 + a positive, or a non-negative, or a non-negative-1 is a positive
                return POS;
            }
        }
        return UNKNOWN;
    }

    /**
     * getAnnotationForPlus handles the following cases (cases 10-12 above):
     *
     * <pre>
     *      8. lit -2 + pos &rarr; gte-1
     *      lit -1 + * &rarr; call demote
     *      lit 0 + * &rarr; *
     *      lit 1 + * &rarr; call promote
     *      9. lit &ge; 2 + {gte-1, nn, or pos} &rarr; pos
     *      let all other lits, including sets, fall through:
     *      10. pos + pos &rarr; pos
     *      11. nn + * &rarr; *
     *      12. pos + gte-1 &rarr; nn
     *      * + * &rarr; lbu
     *  </pre>
     */
    private AnnotationMirror getAnnotationForPlus(
            BinaryOperationNode binaryOpNode, TransferInput<CFValue, CFStore> p) {

        Node leftExprNode = binaryOpNode.getLeftOperand();
        Node rightExprNode = binaryOpNode.getRightOperand();

        AnnotationMirror leftAnno = getLowerBoundAnnotation(leftExprNode, p);

        // Check if the right side's value is known at compile time.
        Long valRight =
                ValueCheckerUtils.getExactValue(
                        rightExprNode.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return getAnnotationForLiteralPlus(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(rightExprNode, p);

        // Check if the left side's value is known at compile time.
        Long valLeft =
                ValueCheckerUtils.getExactValue(
                        leftExprNode.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (valLeft != null) {
            return getAnnotationForLiteralPlus(valLeft.intValue(), rightAnno);
        }

        /* This section is handling the generic cases:
         *      pos + pos -> pos
         *      nn + * -> *
         *      pos + gte-1 -> nn
         */
        if (aTypeFactory.areSameByClass(leftAnno, Positive.class)
                && aTypeFactory.areSameByClass(rightAnno, Positive.class)) {
            return POS;
        }

        if (aTypeFactory.areSameByClass(leftAnno, NonNegative.class)) {
            return rightAnno;
        }

        if (aTypeFactory.areSameByClass(rightAnno, NonNegative.class)) {
            return leftAnno;
        }

        if ((isPositive(leftAnno) && isGTEN1(rightAnno))
                || (isGTEN1(leftAnno) && isPositive(rightAnno))) {
            return NN;
        }
        return UNKNOWN;
    }

    /**
     * getAnnotationForMinus handles the following cases:
     *
     * <pre>
     *      * - lit &rarr; call plus(*, -1 * the value of the lit)
     *      * - * &rarr; lbu
     *      13. if the LessThan type checker can establish that the left side of the expression is &gt; the right side,
     *      returns POS.
     *      14. if the LessThan type checker can establish that the left side of the expression is &ge; the right side,
     *      returns NN.
     *      15. special handling for when the left side is the length of an array or String that's stored as a field,
     *      and the right side is a compile time constant. Do we need this?
     *  </pre>
     */
    private AnnotationMirror getAnnotationForMinus(
            BinaryOperationNode minusNode, TransferInput<CFValue, CFStore> p) {

        // Check if the right side's value is known at compile time.
        Long valRight =
                ValueCheckerUtils.getExactValue(
                        minusNode.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            AnnotationMirror leftAnno = getLowerBoundAnnotation(minusNode.getLeftOperand(), p);
            // Instead of a separate method for subtraction, add the negative of a constant.
            AnnotationMirror result =
                    getAnnotationForLiteralPlus(-1 * valRight.intValue(), leftAnno);

            Tree leftExpr = minusNode.getLeftOperand().getTree();
            Integer minLen = null;
            // Check if the left side is a field access of an array's length,
            // or invocation of String.length. If so,
            // try to look up the MinLen of the array, and potentially keep
            // this either NN or POS instead of GTEN1 or LBU.
            if (leftExpr.getKind() == Tree.Kind.MEMBER_SELECT) {
                MemberSelectTree mstree = (MemberSelectTree) leftExpr;
                minLen = aTypeFactory.getMinLenFromMemberSelectTree(mstree);
            } else if (leftExpr.getKind() == Tree.Kind.METHOD_INVOCATION) {
                MethodInvocationTree mitree = (MethodInvocationTree) leftExpr;
                minLen = aTypeFactory.getMinLenFromMethodInvocationTree(mitree);
            }

            if (minLen != null) {
                result = aTypeFactory.anmFromVal(minLen - valRight);
            }
            return result;
        }

        OffsetEquation leftExpression =
                OffsetEquation.createOffsetFromNode(minusNode.getLeftOperand(), aTypeFactory, '+');
        if (leftExpression != null) {
            if (aTypeFactory
                    .getLessThanAnnotatedTypeFactory()
                    .isLessThan(minusNode.getRightOperand().getTree(), leftExpression.toString())) {
                return POS;
            }

            if (aTypeFactory
                    .getLessThanAnnotatedTypeFactory()
                    .isLessThanOrEqual(
                            minusNode.getRightOperand().getTree(), leftExpression.toString())) {
                return NN;
            }
        }

        // The checker can't reason about arbitrary (i.e. non-literal)
        // things that are being subtracted, so it gives up.
        return UNKNOWN;
    }

    /**
     * Helper function for getAnnotationForMultiply. Handles compile-time known constants.
     *
     * @param val the integer value of the constant
     * @param nonLiteralType the type of the side of the expression that isn't a constant
     */
    private AnnotationMirror getAnnotationForLiteralMultiply(
            int val, AnnotationMirror nonLiteralType) {
        if (val == 0) {
            return NN;
        } else if (val == 1) {
            return nonLiteralType;
        } else if (val > 1) {
            if (isNonNegative(nonLiteralType)) {
                return nonLiteralType;
            }
        }
        return UNKNOWN;
    }

    /**
     * getAnnotationForMultiply handles the following cases:
     *
     * <pre>
     *        * * lit 0 &rarr; nn (=0)
     *        16. * * lit 1 &rarr; *
     *        17. pos * pos &rarr; pos
     *        18. pos * nn &rarr; nn
     *        19. nn * nn &rarr; nn
     *        * * * &rarr; lbu
     *  </pre>
     *
     * Also handles a special case involving Math.random (case 20).
     */
    private AnnotationMirror getAnnotationForMultiply(
            NumericalMultiplicationNode node, TransferInput<CFValue, CFStore> p) {

        // Special handling for multiplying an array length by a Math.random().
        AnnotationMirror randomSpecialCaseResult = aTypeFactory.checkForMathRandomSpecialCase(node);
        if (randomSpecialCaseResult != null) {
            return randomSpecialCaseResult;
        }

        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);

        // Check if the right side's value is known at compile time.
        Long valRight =
                ValueCheckerUtils.getExactValue(
                        node.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return getAnnotationForLiteralMultiply(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);
        // Check if the left side's value is known at compile time.
        Long valLeft =
                ValueCheckerUtils.getExactValue(
                        node.getLeftOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valLeft != null) {
            return getAnnotationForLiteralMultiply(valLeft.intValue(), rightAnno);
        }

        /* This section handles generic annotations:
         *   pos * pos -> pos
         *   nn * pos -> nn (elided, since positives are also non-negative)
         *   nn * nn -> nn
         */
        if (isPositive(leftAnno) && isPositive(rightAnno)) {
            return POS;
        }
        if (isNonNegative(leftAnno) && isNonNegative(rightAnno)) {
            return NN;
        }
        return UNKNOWN;
    }

    /** When the value on the left is known at compile time. */
    private AnnotationMirror addAnnotationForLiteralDivideLeft(
            int val, AnnotationMirror rightAnno) {
        if (val == 0) {
            return NN;
        } else if (val == 1) {
            if (isNonNegative(rightAnno)) {
                return NN;
            } else {
                // (1 / x) can't be outside the range [-1, 1] when x is an integer.
                return GTEN1;
            }
        }
        return UNKNOWN;
    }

    /** When the value on the right is known at compile time. */
    private AnnotationMirror addAnnotationForLiteralDivideRight(
            int val, AnnotationMirror leftAnno) {
        if (val == 0) {
            // Reaching this indicates a divide by zero error. If the value is zero, then this is
            // division by zero. Division by zero is treated as bottom so that users aren't warned
            // about dead code that's dividing by zero. This code assumes that non-dead code won't
            // include literal divide by zeros...
            return aTypeFactory.BOTTOM;
        } else if (val == 1) {
            return leftAnno;
        } else if (val >= 2) {
            if (isNonNegative(leftAnno)) {
                return NN;
            }
        }
        return UNKNOWN;
    }

    /**
     * getAnnotationForDivide handles the following cases (21-26).
     *
     * <pre>
     *      lit 0 / * &rarr; nn (=0)
     *      * / lit 0 &rarr; pos
     *      lit 1 / {pos, nn} &rarr; nn
     *      lit 1 / * &rarr; gten1
     *      * / lit 1 &rarr; *
     *      {pos, nn} / lit &gt;1 &rarr; nn
     *      pos / {pos, nn} &rarr; nn (can round to zero)
     *      * / {pos, nn} &rarr; *
     *      * / * &rarr; lbu
     *  </pre>
     */
    private AnnotationMirror getAnnotationForDivide(
            IntegerDivisionNode node, TransferInput<CFValue, CFStore> p) {

        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);

        // Check if the right side's value is known at compile time.
        Long valRight =
                ValueCheckerUtils.getExactValue(
                        node.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return addAnnotationForLiteralDivideRight(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);

        // Check if the left side's value is known at compile time.
        Long valLeft =
                ValueCheckerUtils.getExactValue(
                        node.getLeftOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valLeft != null) {
            return addAnnotationForLiteralDivideLeft(valLeft.intValue(), leftAnno);
        }

        /* This section handles generic annotations:
         *    pos / {pos, nn} -> nn (can round to zero)
         *    * / {pos, nn} -> *
         */
        if (isPositive(leftAnno) && isNonNegative(rightAnno)) {
            return NN;
        }
        if (isNonNegative(rightAnno)) {
            return leftAnno;
        }
        // Everything else is unknown.
        return UNKNOWN;
    }

    /** A remainder with 1 or -1 as the divisor always results in zero. */
    private AnnotationMirror addAnnotationForLiteralRemainder(int val) {
        if (val == 1 || val == -1) {
            return NN;
        }
        return UNKNOWN;
    }

    /** Adds a default NonNegative annotation to every character. Implements case 33. */
    @Override
    protected void addInformationFromPreconditions(
            CFStore info,
            AnnotatedTypeFactory factory,
            UnderlyingAST.CFGMethod method,
            MethodTree methodTree,
            ExecutableElement methodElement) {
        super.addInformationFromPreconditions(info, factory, method, methodTree, methodElement);

        List<? extends VariableTree> paramTrees = methodTree.getParameters();

        for (VariableTree variableTree : paramTrees) {
            if (TreeUtils.typeOf(variableTree).getKind() == TypeKind.CHAR) {
                JavaExpression je = JavaExpression.fromVariableTree(variableTree);
                info.insertValuePermitNondeterministic(je, aTypeFactory.NN);
            }
        }
    }

    /**
     * getAnnotationForRemainder handles these cases:
     *
     * <pre>
     *      27. * % 1/-1 &rarr; nn
     *      28. pos/nn % * &rarr; nn
     *      29. gten1 % * &rarr; gten1
     *      * % * &rarr; lbu
     * </pre>
     */
    public AnnotationMirror getAnnotationForRemainder(
            IntegerRemainderNode node, TransferInput<CFValue, CFStore> p) {

        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);

        // Check if the right side's value is known at compile time.
        Long valRight =
                ValueCheckerUtils.getExactValue(
                        node.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return addAnnotationForLiteralRemainder(valRight.intValue());
        }

        /* This section handles generic annotations:
              pos/nn % * -> nn
              gten1 % * -> gten1
        */
        if (isNonNegative(leftAnno)) {
            return NN;
        }
        if (isGTEN1(leftAnno)) {
            return GTEN1;
        }

        // Everything else is unknown.
        return UNKNOWN;
    }

    /** Handles shifts (case 30). * &gt;&gt; NonNegative &rarr; NonNegative */
    private AnnotationMirror getAnnotationForRightShift(
            BinaryOperationNode node, TransferInput<CFValue, CFStore> p) {
        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);
        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);

        if (isNonNegative(leftAnno)) {
            if (isNonNegative(rightAnno)) {
                return NN;
            }
        }
        return UNKNOWN;
    }

    /**
     * Handles masking (case 31). Particularly, handles the following cases: * &amp; NonNegative
     * &rarr; NonNegative
     */
    private AnnotationMirror getAnnotationForAnd(
            BitwiseAndNode node, TransferInput<CFValue, CFStore> p) {

        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);
        if (isNonNegative(rightAnno)) {
            return NN;
        }

        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);
        if (isNonNegative(leftAnno)) {
            return NN;
        }
        return UNKNOWN;
    }

    /**
     * Returns true if the argument is the @Positive type annotation.
     *
     * @param anm the annotation to test
     * @return true if the the argument is the @Positive type annotation
     */
    private boolean isPositive(AnnotationMirror anm) {
        return aTypeFactory.areSameByClass(anm, Positive.class);
    }

    /**
     * Returns true if the argument is the @NonNegative type annotation (or a stronger one).
     *
     * @param anm the annotation to test
     * @return true if the the argument is the @NonNegative type annotation
     */
    private boolean isNonNegative(AnnotationMirror anm) {
        return aTypeFactory.areSameByClass(anm, NonNegative.class) || isPositive(anm);
    }

    /**
     * Returns true if the argument is the @GTENegativeOne type annotation (or a stronger one).
     *
     * @param anm the annotation to test
     * @return true if the the argument is the @GTENegativeOne type annotation
     */
    private boolean isGTEN1(AnnotationMirror anm) {
        return aTypeFactory.areSameByClass(anm, GTENegativeOne.class) || isNonNegative(anm);
    }

    private AnnotationMirror getLowerBoundAnnotation(
            Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        return getLowerBoundAnnotation(value);
    }

    private AnnotationMirror getLowerBoundAnnotation(CFValue cfValue) {
        return aTypeFactory
                .getQualifierHierarchy()
                .findAnnotationInHierarchy(cfValue.getAnnotations(), aTypeFactory.UNKNOWN);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(
            NumericalAdditionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalAddition(n, p);
        AnnotationMirror newAnno = getAnnotationForPlus(n, p);
        return createNewResult(result, newAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalSubtraction(n, p);
        AnnotationMirror newAnno = getAnnotationForMinus(n, p);
        return createNewResult(result, newAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMultiplication(
            NumericalMultiplicationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitNumericalMultiplication(n, p);
        AnnotationMirror newAnno = getAnnotationForMultiply(n, p);
        return createNewResult(result, newAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerDivision(
            IntegerDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitIntegerDivision(n, p);
        AnnotationMirror newAnno = getAnnotationForDivide(n, p);
        return createNewResult(result, newAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerRemainder(
            IntegerRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitIntegerRemainder(n, p);
        AnnotationMirror resultAnno = getAnnotationForRemainder(n, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSignedRightShift(
            SignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitSignedRightShift(n, p);
        AnnotationMirror resultAnno = getAnnotationForRightShift(n, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitUnsignedRightShift(
            UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitUnsignedRightShift(n, p);
        AnnotationMirror resultAnno = getAnnotationForRightShift(n, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseAnd(
            BitwiseAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseAnd(n, p);
        AnnotationMirror resultAnno = getAnnotationForAnd(n, p);
        return createNewResult(transferResult, resultAnno);
    }

    /**
     * Create a new transfer result based on the original result and the new annotation.
     *
     * @param result the original result
     * @param resultAnno the new annotation
     * @return the new transfer result
     */
    private TransferResult<CFValue, CFStore> createNewResult(
            TransferResult<CFValue, CFStore> result, AnnotationMirror resultAnno) {
        CFValue newResultValue =
                analysis.createSingleAnnotationValue(
                        resultAnno, result.getResultValue().getUnderlyingType());
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }
}
