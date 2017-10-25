package org.checkerframework.checker.index.lowerbound;

import static org.checkerframework.checker.index.IndexUtil.getExactValue;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LowerBoundUnknown;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
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
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;

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
     *
     * @param mLiteral a potential literal
     * @param otherNode the node on the other side of the ==/!=
     * @param otherAnno the annotation of the other side of the ==/!=
     */
    private void notEqualToValue(
            Node mLiteral, Node otherNode, AnnotationMirror otherAnno, CFStore store) {

        Long integerLiteral =
                getExactValue(mLiteral.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());

        if (integerLiteral == null) {
            return;
        }
        long intLiteral = integerLiteral.longValue();

        if (intLiteral == 0) {
            if (AnnotationUtils.areSameByClass(otherAnno, NonNegative.class)) {
                List<Node> internals = splitAssignments(otherNode);
                for (Node internal : internals) {
                    Receiver rec = FlowExpressions.internalReprOf(aTypeFactory, internal);
                    store.insertValue(rec, POS);
                }
            }
        } else if (intLiteral == -1) {
            if (AnnotationUtils.areSameByClass(otherAnno, GTENegativeOne.class)) {
                List<Node> internals = splitAssignments(otherNode);
                for (Node internal : internals) {
                    Receiver rec = FlowExpressions.internalReprOf(aTypeFactory, internal);
                    store.insertValue(rec, NN);
                }
            }
        }
    }

    /** Implements the transfer rules for both equal nodes and not-equals nodes. */
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

        return rfi.newResult;
    }

    /**
     * The implementation of the algorithm for refining a &gt; test. Changes the type of left (the
     * greater one) to one closer to bottom than the type of right. Can't call the promote function
     * from the ATF directly because a new expression isn't introduced here - the modifications have
     * to be made to an existing one.
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

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        if (AnnotationUtils.areSame(rightAnno, GTEN1)) {
            store.insertValue(leftRec, NN);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, NN)) {
            store.insertValue(leftRec, POS);
            return;
        }
        if (AnnotationUtils.areSame(rightAnno, POS)) {
            store.insertValue(leftRec, POS);
            return;
        }
    }

    /**
     * Refines left to exactly the level of right, since in the worst case they're equal. Modifies
     * an existing type in the store, but has to be careful not to overwrite a more precise existing
     * type.
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

        Receiver leftRec = FlowExpressions.internalReprOf(aTypeFactory, left);

        AnnotationMirror newLBType =
                aTypeFactory.getQualifierHierarchy().greatestLowerBound(rightAnno, leftAnno);

        store.insertValue(leftRec, newLBType);
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
     * Helper method for getAnnotationForPlus. Handles addition of constants.
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
     * getAnnotationForPlus handles the following cases:
     *
     * <pre>
     *      lit -2 + pos &rarr; gte-1
     *      lit -1 + * &rarr; call demote
     *      lit 0 + * &rarr; *
     *      lit 1 + * &rarr; call promote
     *      lit &ge; 2 + {gte-1, nn, or pos} &rarr; pos
     *      let all other lits, including sets, fall through:
     *      pos + pos &rarr; pos
     *      nn + * &rarr; *
     *      pos + gte-1 &rarr; nn
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
                getExactValue(rightExprNode.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return getAnnotationForLiteralPlus(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(rightExprNode, p);

        // Check if the left side's value is known at compile time.
        Long valLeft =
                getExactValue(leftExprNode.getTree(), aTypeFactory.getValueAnnotatedTypeFactory());
        if (valLeft != null) {
            return getAnnotationForLiteralPlus(valLeft.intValue(), rightAnno);
        }

        /* This section is handling the generic cases:
         *      pos + pos -> pos
         *      nn + * -> *
         *      pos + gte-1 -> nn
         */
        if (AnnotationUtils.areSameByClass(leftAnno, Positive.class)
                && AnnotationUtils.areSameByClass(rightAnno, Positive.class)) {
            return POS;
        }

        if (AnnotationUtils.areSameByClass(leftAnno, NonNegative.class)) {
            return rightAnno;
        }

        if (AnnotationUtils.areSameByClass(rightAnno, NonNegative.class)) {
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
     *  </pre>
     */
    private AnnotationMirror getAnnotationForMinus(
            BinaryOperationNode minusNode, TransferInput<CFValue, CFStore> p) {

        // Check if the right side's value is known at compile time.
        Long valRight =
                getExactValue(
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
     *        * * lit 1 &rarr; *
     *        pos * pos &rarr; pos
     *        pos * nn &rarr; nn
     *        nn * nn &rarr; nn
     *        * * * &rarr; lbu
     *  </pre>
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
                getExactValue(
                        node.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return getAnnotationForLiteralMultiply(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);
        // Check if the left side's value is known at compile time.
        Long valLeft =
                getExactValue(
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
            // division by zero. Division by zero is treated as bottom so that users
            // aren't warned about dead code that's dividing by zero. This code assumes that non-dead
            // code won't include literal divide by zeros...
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
     * getAnnotationForDivide handles these cases:
     *
     * <pre>
     * lit 0 / * &rarr; nn (=0)
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
                getExactValue(
                        node.getRightOperand().getTree(),
                        aTypeFactory.getValueAnnotatedTypeFactory());
        if (valRight != null) {
            return addAnnotationForLiteralDivideRight(valRight.intValue(), leftAnno);
        }

        AnnotationMirror rightAnno = getLowerBoundAnnotation(node.getRightOperand(), p);

        // Check if the left side's value is known at compile time.
        Long valLeft =
                getExactValue(
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

    /**
     * getAnnotationForRemainder handles these cases: * % 1/-1 &rarr; nn pos/nn % * &rarr; nn gten1
     * % * &rarr; gten1 * % * &rarr; lbu
     */
    public AnnotationMirror getAnnotationForRemainder(
            IntegerRemainderNode node, TransferInput<CFValue, CFStore> p) {

        AnnotationMirror leftAnno = getLowerBoundAnnotation(node.getLeftOperand(), p);

        // Check if the right side's value is known at compile time.
        Long valRight =
                getExactValue(
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

    /** Handles shifts. * &gt;&gt; NonNegative &rarr; NonNegative */
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
     * Handles masking. Particularly, handles the following cases: * &amp; NonNegative &rarr;
     * NonNegative
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

    private boolean isPositive(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, Positive.class);
    }

    private boolean isNonNegative(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, NonNegative.class) || isPositive(anm);
    }

    private boolean isGTEN1(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, GTENegativeOne.class) || isNonNegative(anm);
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
