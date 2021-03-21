package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.index.Subsequence;
import org.checkerframework.checker.index.inequality.LessThanAnnotatedTypeFactory;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SubstringIndexFor;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.checker.index.upperbound.UBQualifier.UpperBoundUnknownQualifier;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.CaseNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Contains the transfer functions for the upper bound type system, a part of the Index Checker.
 * This class implements the following refinement rules:
 *
 * <ul>
 *   <li>1. Refine the type of expressions used as an array dimension to be less than length of the
 *       array to which the new array is assigned. For example, in {@code int[] array = new
 *       int[expr];}, the type of expr is {@code @LTEqLength("array")}.
 *   <li>2. If {@code other * node} has type {@code typeOfMultiplication}, then if {@code other} is
 *       positive, then {@code node} is {@code typeOfMultiplication}.
 *   <li>3. If {@code other * node} has type {@code typeOfMultiplication}, if {@code other} is
 *       greater than 1, then {@code node} is {@code typeOfMultiplication} plus 1.
 *   <li>4. Given a subtraction node, {@code node}, that is known to have type {@code
 *       typeOfSubtraction}. An offset can be applied to the left node (i.e. the left node has the
 *       same type, but with an offset based on the right node).
 *   <li>5. In an addition expression, refine the two operands based on the type of the whole
 *       expression with appropriate offsets.
 *   <li>6. If an addition expression has a type that is less than length of an array, and one of
 *       the operands is non-negative, then the other is less than or equal to the length of the
 *       array.
 *   <li>7. If an addition expression has a type that is less than length of an array, and one of
 *       the operands is positive, then the other is also less than the length of the array.
 *   <li>8. if x &lt; y, and y has a type that is related to the length of an array, then x has the
 *       same type, with an offset that is one less.
 *   <li>9. if x &le; y, and y has a type that is related to the length of an array, then x has the
 *       same type.
 *   <li>10. refine the subtrahend in a subtraction which is greater than or equal to a certain
 *       offset. The type of the subtrahend is refined to the type of the minuend with the offset
 *       added.
 *   <li>11. if two variables are equal, they have the same type
 *   <li>12. If one node in a != expression is an sequence length field or method access (optionally
 *       with a constant offset subtracted) and the other node is less than or equal to that
 *       sequence length (minus the offset), then refine the other node's type to less than the
 *       sequence length (minus the offset).
 *   <li>13. If some Node a is known to be less than the length of some array, x, then, the type of
 *       a + b, is {@code @LTLengthOf(value="x", offset="-b")}. If b is known to be less than the
 *       length of some other array, y, then the type of a + b is {@code @LTLengthOf(value={"x",
 *       "y"}, offset={"-b", "-a"})}.
 *   <li>14. If a is known to be less than the length of x when some offset, o, is added to a (the
 *       type of a is {@code @LTLengthOf(value="x", offset="o"))}, then the type of a + b is
 *       {@code @LTLengthOf(value="x",offset="o - b")}. (Note, if "o - b" can be computed, then it
 *       is and the result is used in the annotation.)
 *   <li>15. If expression i has type {@code @LTLengthOf(value = "f2", offset = "f1.length")} int
 *       and expression j is less than or equal to the length of f1, then the type of i + j is
 *       {@code @LTLengthOf("f2")}.
 *   <li>16. If some Node a is known to be less than the length of some sequence x, then the type of
 *       a - b is {@code @LTLengthOf(value="x", offset="b")}.
 *   <li>17. If some Node a is known to be less than the length of some sequence x, and if b is
 *       non-negative or positive, then a - b should keep the types of a.
 *   <li>18. The type of a sequence length access (i.e. array.length) is
 *       {@code @LTLength(value={"array"...}, offset="-1")} where "array"... is the set of all
 *       sequences that are the same length (via the SameLen checker) as "array"
 *   <li>19. If n is an array length field access, then the type of a.length is the glb of
 *       {@code @LTEqLengthOf("a")} and the value of a.length in the store.
 *   <li>20. If n is a String.length() method invocation, then the type of s.length() is the glb of
 *       {@code @LTEqLengthOf("s")} and the value of s.length() in the store.
 * </ul>
 */
public class UpperBoundTransfer extends IndexAbstractTransfer {

    /** The type factory associated with this transfer function. */
    private UpperBoundAnnotatedTypeFactory atypeFactory;

    /**
     * Creates a new UpperBoundTransfer.
     *
     * @param analysis the analysis for this transfer function
     */
    public UpperBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * Case 1: Refine the type of expressions used as an array dimension to be less than length of
     * the array to which the new array is assigned. For example, in "int[] array = new int[expr];",
     * the type of expr is @LTEqLength("array").
     */
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        Node expNode = node.getExpression();

        // strip off typecast if any
        Node expNodeSansCast =
                (expNode instanceof TypeCastNode) ? ((TypeCastNode) expNode).getOperand() : expNode;
        // null if right-hand-side is not an array creation expression
        ArrayCreationNode acNode =
                (expNodeSansCast instanceof ArrayCreationNode)
                        ? acNode = (ArrayCreationNode) expNodeSansCast
                        : null;

        if (acNode != null) {
            // Right-hand side of assignment is an array creation expression
            List<Node> nodeList = acNode.getDimensions();
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);

            UBQualifier previousQualifier = getUBQualifier(dim, in);
            JavaExpression arrayExpr = JavaExpression.fromNode(node.getTarget());
            String arrayString = arrayExpr.toString();
            LessThanLengthOf newInfo =
                    (LessThanLengthOf) UBQualifier.createUBQualifier(arrayString, "-1");
            UBQualifier combined = previousQualifier.glb(newInfo);
            AnnotationMirror newAnno = atypeFactory.convertUBQualifierToAnnotation(combined);

            JavaExpression dimExpr = JavaExpression.fromNode(dim);
            result.getRegularStore().insertValue(dimExpr, newAnno);
            propagateToOperands(newInfo, dim, in, result.getRegularStore());
        }
        return result;
    }

    /**
     * {@code node} is known to be {@code typeOfNode}. If the node is a plus or a minus then the
     * types of the left and right operands can be refined to include offsets. If the node is a
     * multiplication, its operands can also be refined. See {@link
     * #propagateToAdditionOperand(LessThanLengthOf, Node, Node, TransferInput, CFStore)}, {@link
     * #propagateToSubtractionOperands(LessThanLengthOf, NumericalSubtractionNode, TransferInput,
     * CFStore)}, and {@link #propagateToMultiplicationOperand(LessThanLengthOf, Node, Node,
     * TransferInput, CFStore)} for details.
     */
    private void propagateToOperands(
            LessThanLengthOf typeOfNode,
            Node node,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        if (node instanceof NumericalAdditionNode) {
            Node right = ((NumericalAdditionNode) node).getRightOperand();
            Node left = ((NumericalAdditionNode) node).getLeftOperand();
            propagateToAdditionOperand(typeOfNode, left, right, in, store);
            propagateToAdditionOperand(typeOfNode, right, left, in, store);
        } else if (node instanceof NumericalSubtractionNode) {
            propagateToSubtractionOperands(typeOfNode, (NumericalSubtractionNode) node, in, store);
        } else if (node instanceof NumericalMultiplicationNode) {
            if (atypeFactory.hasLowerBoundTypeByClass(node, Positive.class)) {
                Node right = ((NumericalMultiplicationNode) node).getRightOperand();
                Node left = ((NumericalMultiplicationNode) node).getLeftOperand();
                propagateToMultiplicationOperand(typeOfNode, left, right, in, store);
                propagateToMultiplicationOperand(typeOfNode, right, left, in, store);
            }
        }
    }

    /**
     * {@code other} times {@code node} is known to be {@code typeOfMultiplication}.
     *
     * <p>This implies that if {@code other} is positive, then {@code node} is {@code
     * typeOfMultiplication}. If {@code other} is greater than 1, then {@code node} is {@code
     * typeOfMultiplication} plus 1. These are cases 2 and 3, respectively.
     */
    private void propagateToMultiplicationOperand(
            LessThanLengthOf typeOfMultiplication,
            Node node,
            Node other,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        if (atypeFactory.hasLowerBoundTypeByClass(other, Positive.class)) {
            Long minValue =
                    ValueCheckerUtils.getMinValue(
                            other.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
            if (minValue != null && minValue > 1) {
                typeOfMultiplication = (LessThanLengthOf) typeOfMultiplication.plusOffset(1);
            }
            UBQualifier qual = getUBQualifier(node, in);
            UBQualifier newQual = qual.glb(typeOfMultiplication);
            JavaExpression je = JavaExpression.fromNode(node);
            store.insertValue(je, atypeFactory.convertUBQualifierToAnnotation(newQual));
        }
    }

    /**
     * The subtraction node, {@code node}, is known to be {@code typeOfSubtraction}.
     *
     * <p>This means that the left node is less than or equal to the length of the array when the
     * right node is subtracted from the left node. Note that unlike {@link
     * #propagateToAdditionOperand(LessThanLengthOf, Node, Node, TransferInput, CFStore)} and {@link
     * #propagateToMultiplicationOperand(LessThanLengthOf, Node, Node, TransferInput, CFStore)},
     * this method takes the NumericalSubtractionNode instead of the two operand nodes. This
     * implements case 4.
     *
     * @param typeOfSubtraction type of node
     * @param node subtraction node that has typeOfSubtraction
     * @param in a TransferInput
     * @param store location to store the refined type
     */
    private void propagateToSubtractionOperands(
            LessThanLengthOf typeOfSubtraction,
            NumericalSubtractionNode node,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        UBQualifier left = getUBQualifier(node.getLeftOperand(), in);
        UBQualifier newInfo = typeOfSubtraction.minusOffset(node.getRightOperand(), atypeFactory);

        UBQualifier newLeft = left.glb(newInfo);
        JavaExpression leftJe = JavaExpression.fromNode(node.getLeftOperand());
        store.insertValue(leftJe, atypeFactory.convertUBQualifierToAnnotation(newLeft));
    }

    /**
     * Refines the type of {@code operand} to {@code typeOfAddition} plus {@code other}. If {@code
     * other} is non-negative, then {@code operand} also less than the length of the arrays in
     * {@code typeOfAddition}. If {@code other} is positive, then {@code operand} also less than the
     * length of the arrays in {@code typeOfAddition} plus 1. These are cases 5, 6, and 7.
     *
     * @param typeOfAddition type of {@code operand + other}
     * @param operand the Node to refine
     * @param other the Node added to {@code operand}
     * @param in a TransferInput
     * @param store location to store the refined types
     */
    private void propagateToAdditionOperand(
            LessThanLengthOf typeOfAddition,
            Node operand,
            Node other,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        UBQualifier operandQual = getUBQualifier(operand, in);
        UBQualifier newQual = operandQual.glb(typeOfAddition.plusOffset(other, atypeFactory));

        /** If the node is NN, add an LTEL to the qual. If POS, add an LTL. */
        if (atypeFactory.hasLowerBoundTypeByClass(other, Positive.class)) {
            newQual = newQual.glb(typeOfAddition.plusOffset(1));
        } else if (atypeFactory.hasLowerBoundTypeByClass(other, NonNegative.class)) {
            newQual = newQual.glb(typeOfAddition);
        }
        JavaExpression operandJe = JavaExpression.fromNode(operand);
        store.insertValue(operandJe, atypeFactory.convertUBQualifierToAnnotation(newQual));
    }

    /**
     * Case 8: if x &lt; y, and y has a type that is related to the length of an array, then x has
     * the same type, with an offset that is one less.
     */
    @Override
    protected void refineGT(
            Node larger,
            AnnotationMirror largerAnno,
            Node smaller,
            AnnotationMirror smallerAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        // larger > smaller
        UBQualifier largerQual =
                UBQualifier.createUBQualifier(
                        largerAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        // larger + 1 >= smaller
        UBQualifier largerQualPlus1 = largerQual.plusOffset(1);
        UBQualifier rightQualifier =
                UBQualifier.createUBQualifier(
                        smallerAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        UBQualifier refinedRight = rightQualifier.glb(largerQualPlus1);

        if (largerQualPlus1.isLessThanLengthQualifier()) {
            propagateToOperands((LessThanLengthOf) largerQualPlus1, smaller, in, store);
        }

        refineSubtrahendWithOffset(larger, smaller, true, in, store);

        JavaExpression rightJe = JavaExpression.fromNode(smaller);
        store.insertValue(rightJe, atypeFactory.convertUBQualifierToAnnotation(refinedRight));
    }

    /**
     * Case 9: if x &le; y, and y has a type that is related to the length of an array, then x has
     * the same type.
     */
    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        UBQualifier leftQualifier =
                UBQualifier.createUBQualifier(
                        leftAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        UBQualifier rightQualifier =
                UBQualifier.createUBQualifier(
                        rightAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        UBQualifier refinedRight = rightQualifier.glb(leftQualifier);

        if (leftQualifier.isLessThanLengthQualifier()) {
            propagateToOperands((LessThanLengthOf) leftQualifier, right, in, store);
        }

        refineSubtrahendWithOffset(left, right, false, in, store);

        JavaExpression rightJe = JavaExpression.fromNode(right);
        store.insertValue(rightJe, atypeFactory.convertUBQualifierToAnnotation(refinedRight));
    }

    /**
     * Refines the subtrahend in a subtraction which is greater than or equal to a certain offset.
     * The type of the subtrahend is refined to the type of the minuend with the offset added. This
     * is case 10.
     *
     * <p>This is based on the fact that if {@code (minuend - subtrahend) >= offset}, and {@code
     * minuend + o < l}, then {@code subtrahend + o + offset < l}.
     *
     * <p>If {@code gtNode} is not a {@link NumericalSubtractionNode}, the method does nothing.
     *
     * @param gtNode the node that is greater or equal to the offset
     * @param offsetNode a node part of the offset
     * @param offsetAddOne whether to add one to the offset
     * @param in input of the transfer function
     * @param store location to store the refined types
     */
    private void refineSubtrahendWithOffset(
            Node gtNode,
            Node offsetNode,
            boolean offsetAddOne,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        if (gtNode instanceof NumericalSubtractionNode) {
            NumericalSubtractionNode subtractionNode = (NumericalSubtractionNode) gtNode;

            Node minuend = subtractionNode.getLeftOperand();
            UBQualifier minuendQual = getUBQualifier(minuend, in);
            Node subtrahend = subtractionNode.getRightOperand();
            UBQualifier subtrahendQual = getUBQualifier(subtrahend, in);

            UBQualifier newQual =
                    subtrahendQual.glb(
                            minuendQual
                                    .plusOffset(offsetNode, atypeFactory)
                                    .plusOffset(offsetAddOne ? 1 : 0));
            JavaExpression subtrahendJe = JavaExpression.fromNode(subtrahend);
            store.insertValue(subtrahendJe, atypeFactory.convertUBQualifierToAnnotation(newQual));
        }
    }

    /** Implements case 11. */
    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> res,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        TransferResult<CFValue, CFStore> result =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
        IndexRefinementInfo rfi = new IndexRefinementInfo(result, analysis, firstNode, secondNode);
        if (rfi.leftAnno == null || rfi.rightAnno == null) {
            return result;
        }

        CFStore equalsStore = notEqualTo ? rfi.elseStore : rfi.thenStore;
        CFStore notEqualStore = notEqualTo ? rfi.thenStore : rfi.elseStore;

        refineEq(rfi.left, rfi.leftAnno, rfi.right, rfi.rightAnno, equalsStore);

        refineNeqSequenceLength(rfi.left, rfi.right, rfi.rightAnno, notEqualStore);
        refineNeqSequenceLength(rfi.right, rfi.left, rfi.leftAnno, notEqualStore);
        return rfi.newResult;
    }

    /** Refines the type of the left and right node to glb of the left and right annotation. */
    private void refineEq(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store) {
        UBQualifier leftQualifier =
                UBQualifier.createUBQualifier(
                        leftAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        UBQualifier rightQualifier =
                UBQualifier.createUBQualifier(
                        rightAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
        UBQualifier glb = rightQualifier.glb(leftQualifier);
        AnnotationMirror glbAnno = atypeFactory.convertUBQualifierToAnnotation(glb);

        List<Node> internalsRight = splitAssignments(right);
        for (Node internal : internalsRight) {
            JavaExpression rightJe = JavaExpression.fromNode(internal);
            store.insertValue(rightJe, glbAnno);
        }

        List<Node> internalsLeft = splitAssignments(left);
        for (Node internal : internalsLeft) {
            JavaExpression leftJe = JavaExpression.fromNode(internal);
            store.insertValue(leftJe, glbAnno);
        }
    }

    /**
     * If lengthAccess node is an sequence length field or method access (optionally with a constant
     * offset subtracted) and the other node is less than or equal to that sequence length (minus
     * the offset), then refine the other node's type to less than the sequence length (minus the
     * offset). This is case 12.
     */
    private void refineNeqSequenceLength(
            Node lengthAccess, Node otherNode, AnnotationMirror otherNodeAnno, CFStore store) {

        // If lengthAccess is "receiver.length - c" where c is an integer constant,
        // then lengthOffset is "c".
        int lengthOffset = 0;
        if (lengthAccess instanceof NumericalSubtractionNode) {
            NumericalSubtractionNode subtraction = (NumericalSubtractionNode) lengthAccess;
            Node offsetNode = subtraction.getRightOperand();
            Long offsetValue =
                    ValueCheckerUtils.getExactValue(
                            offsetNode.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
            if (offsetValue != null
                    && offsetValue > Integer.MIN_VALUE
                    && offsetValue <= Integer.MAX_VALUE) {
                lengthOffset = offsetValue.intValue();
                lengthAccess = subtraction.getLeftOperand();
            } else {
                // Subtraction of non-constant expressions is not supported
                return;
            }
        }

        JavaExpression receiver = null;
        if (NodeUtils.isArrayLengthFieldAccess(lengthAccess)) {
            FieldAccess fa = JavaExpression.fromNodeFieldAccess((FieldAccessNode) lengthAccess);
            receiver = fa.getReceiver();

        } else if (atypeFactory.getMethodIdentifier().isLengthOfMethodInvocation(lengthAccess)) {
            JavaExpression ma = JavaExpression.fromNode(lengthAccess);
            if (ma instanceof MethodCall) {
                receiver = ((MethodCall) ma).getReceiver();
            }
        }

        if (receiver != null && !receiver.containsUnknown()) {
            UBQualifier otherQualifier =
                    UBQualifier.createUBQualifier(
                            otherNodeAnno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
            String sequence = receiver.toString();
            // Check if otherNode + c - 1 < receiver.length
            if (otherQualifier.hasSequenceWithOffset(sequence, lengthOffset - 1)) {
                // Add otherNode + c < receiver.length
                UBQualifier newQualifier =
                        UBQualifier.createUBQualifier(sequence, Integer.toString(lengthOffset));
                otherQualifier = otherQualifier.glb(newQualifier);
                for (Node internal : splitAssignments(otherNode)) {
                    JavaExpression leftJe = JavaExpression.fromNode(internal);
                    store.insertValue(
                            leftJe, atypeFactory.convertUBQualifierToAnnotation(otherQualifier));
                }
            }
        }
    }

    /**
     * If some Node a is known to be less than the length of some array, x, then, the type of a + b,
     * is @LTLengthOf(value="x", offset="-b"). If b is known to be less than the length of some
     * other array, y, then the type of a + b is @LTLengthOf(value={"x", "y"}, offset={"-b", "-a"}).
     *
     * <p>Alternatively, if a is known to be less than the length of x when some offset, o, is added
     * to a (the type of a is @LTLengthOf(value="x", offset="o")), then the type of a + b
     * is @LTLengthOf(value="x",offset="o - b"). (Note, if "o - b" can be computed, then it is and
     * the result is used in the annotation.)
     *
     * <p>In addition, If expression i has type @LTLengthOf(value = "f2", offset = "f1.length") int
     * and expression j is less than or equal to the length of f1, then the type of i + j is
     * .@LTLengthOf("f2").
     *
     * <p>These three cases correspond to cases 13-15.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(
            NumericalAdditionNode n, TransferInput<CFValue, CFStore> in) {
        // type of leftNode + rightNode  is  glb(t, s) where
        // t = minusOffset(type(leftNode), rightNode) and
        // s = minusOffset(type(rightNode), leftNode)

        UBQualifier left = getUBQualifierForAddition(n.getLeftOperand(), in);
        UBQualifier t = left.minusOffset(n.getRightOperand(), atypeFactory);

        UBQualifier right = getUBQualifierForAddition(n.getRightOperand(), in);
        UBQualifier s = right.minusOffset(n.getLeftOperand(), atypeFactory);

        UBQualifier glb = t.glb(s);
        if (left.isLessThanLengthQualifier() && right.isLessThanLengthQualifier()) {
            // If expression i has type @LTLengthOf(value = "f2", offset = "f1.length") int and
            // expression j is less than or equal to the length of f1, then the type of i + j is
            // @LTLengthOf("f2").
            UBQualifier r =
                    removeSequenceLengths((LessThanLengthOf) left, (LessThanLengthOf) right);
            glb = glb.glb(r);
            UBQualifier l =
                    removeSequenceLengths((LessThanLengthOf) right, (LessThanLengthOf) left);
            glb = glb.glb(l);
        }

        return createTransferResult(n, in, glb);
    }

    /**
     * Return the result of adding i to j.
     *
     * <p>When expression i has type {@code @LTLengthOf(value = "f2", offset = "f1.length") int} and
     * expression j is less than or equal to the length of f1, then the type of i + j
     * is @LTLengthOf("f2").
     *
     * <p>When expression i has type {@code @LTLengthOf (value = "f2", offset = "f1.length - 1")
     * int} and expression j is less than the length of f1, then the type of i + j
     * is @LTLengthOf("f2").
     *
     * @param i the type of the expression added to j
     * @param j the type of the expression added to i
     * @return the type of i + j
     */
    private UBQualifier removeSequenceLengths(LessThanLengthOf i, LessThanLengthOf j) {
        List<String> lessThan = new ArrayList<>();
        List<String> lessThanOrEqual = new ArrayList<>();
        for (String sequence : i.getSequences()) {
            if (i.isLessThanLengthOf(sequence)) {
                lessThan.add(sequence);
            } else if (i.hasSequenceWithOffset(sequence, -1)) {
                lessThanOrEqual.add(sequence);
            }
        }
        // Creates a qualifier that is the same a j with the array.length offsets removed. If
        // an offset doesn't have an array.length, then the offset/array pair is removed. If
        // there are no such pairs, Unknown is returned.
        UBQualifier lessThanEqQ = j.removeSequenceLengthAccess(lessThanOrEqual);
        // Creates a qualifier that is the same a j with the array.length - 1 offsets removed. If
        // an offset doesn't have an array.length, then the offset/array pair is removed. If
        // there are no such pairs, Unknown is returned.
        UBQualifier lessThanQ = j.removeSequenceLengthAccessAndNeg1(lessThan);

        return lessThanEqQ.glb(lessThanQ);
    }

    /**
     * If some Node a is known to be less than the length of some sequence x, then the type of a - b
     * is @LTLengthOf(value="x", offset="b"). If b is known to be less than the length of some other
     * sequence, this doesn't add any information about the type of a - b. But, if b is non-negative
     * or positive, then a - b should keep the types of a. This corresponds to cases 16 and 17.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> in) {
        UBQualifier left = getUBQualifier(n.getLeftOperand(), in);
        UBQualifier leftWithOffset = left.plusOffset(n.getRightOperand(), atypeFactory);
        if (atypeFactory.hasLowerBoundTypeByClass(n.getRightOperand(), NonNegative.class)
                || atypeFactory.hasLowerBoundTypeByClass(n.getRightOperand(), Positive.class)) {
            // If the right side of the expression is NN or POS, then all the left side's
            // annotations should be kept.
            if (left.isLessThanLengthQualifier()) {
                leftWithOffset = left.glb(leftWithOffset);
            }
        }

        // If the result of a numerical subtraction would be LTEL(b) or LTL(b), and b is HSS(a,
        // from, to), and the subtraction node itself is i - from where i is LTEL(b), then the
        // result is LTEL(a).  If i is LTL(b) instead, the result is LTL(a).

        if (leftWithOffset.isLessThanLengthQualifier()) {

            LessThanLengthOf subtractionResult = (LessThanLengthOf) leftWithOffset;

            for (String b : subtractionResult.getSequences()) {
                if (subtractionResult.hasSequenceWithOffset(b, -1)
                        || subtractionResult.hasSequenceWithOffset(b, 0)) {

                    TreePath currentPath = this.atypeFactory.getPath(n.getTree());
                    JavaExpression je;
                    try {
                        je =
                                UpperBoundVisitor.parseJavaExpressionString(
                                        b, atypeFactory, currentPath);
                    } catch (NullPointerException npe) {
                        // I have no idea why this seems to happen only on a few JDK classes.
                        // It appears to only happen during the preprocessing step - the NPE
                        // is thrown while trying to find the enclosing class of a class tree,
                        // which is null. I can't find a reproducible
                        // test case that's smaller than the size of DualPivotQuicksort.
                        // Since this refinement is optional, but useful elsewhere, catching this
                        // NPE here and returning is always safe.
                        return createTransferResult(n, in, leftWithOffset);
                    }

                    Subsequence subsequence =
                            Subsequence.getSubsequenceFromReceiver(je, atypeFactory);

                    if (subsequence != null) {
                        String from = subsequence.from;
                        String to = subsequence.to;
                        String a = subsequence.array;

                        JavaExpression leftOp = JavaExpression.fromNode(n.getLeftOperand());
                        JavaExpression rightOp = JavaExpression.fromNode(n.getRightOperand());

                        if (rightOp.toString().equals(from)) {
                            LessThanAnnotatedTypeFactory lessThanAtypeFactory =
                                    atypeFactory.getLessThanAnnotatedTypeFactory();
                            AnnotationMirror lessThanType =
                                    lessThanAtypeFactory
                                            .getAnnotatedType(n.getLeftOperand().getTree())
                                            .getAnnotation(LessThan.class);

                            if (lessThanType != null
                                    && lessThanAtypeFactory.isLessThan(lessThanType, to)) {
                                UBQualifier ltlA = UBQualifier.createUBQualifier(a, "0");
                                leftWithOffset = leftWithOffset.glb(ltlA);
                            } else if (leftOp.toString().equals(to)
                                    || (lessThanType != null
                                            && lessThanAtypeFactory.isLessThanOrEqual(
                                                    lessThanType, to))) {
                                // It's necessary to check if leftOp == to because LessThan doesn't
                                // infer that things are less than or equal to themselves.
                                UBQualifier ltelA = UBQualifier.createUBQualifier(a, "-1");
                                leftWithOffset = leftWithOffset.glb(ltelA);
                            }
                        }
                    }
                }
            }
        }
        return createTransferResult(n, in, leftWithOffset);
    }

    /**
     * Computes a type of a sequence length access. This is case 18.
     *
     * @param n sequence length access node
     */
    private TransferResult<CFValue, CFStore> visitLengthAccess(
            Node n,
            TransferInput<CFValue, CFStore> in,
            JavaExpression sequenceJe,
            Tree sequenceTree) {
        if (sequenceTree == null) {
            return null;
        }
        // Look up the SameLen type of the sequence.
        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromTree(sequenceTree);
        List<String> sameLenSequences;
        if (sameLenAnno == null) {
            sameLenSequences = Collections.singletonList(sequenceJe.toString());
        } else {
            sameLenSequences =
                    AnnotationUtils.getElementValueArray(
                            sameLenAnno, atypeFactory.sameLenValueElement, String.class);
            if (!sameLenSequences.contains(sequenceJe.toString())) {
                sameLenSequences.add(sequenceJe.toString());
            }
        }

        List<String> offsets = Collections.nCopies(sameLenSequences.size(), "-1");

        if (CFAbstractStore.canInsertJavaExpression(sequenceJe)) {
            UBQualifier qualifier = UBQualifier.createUBQualifier(sameLenSequences, offsets);
            UBQualifier previous = getUBQualifier(n, in);
            return createTransferResult(n, in, qualifier.glb(previous));
        }

        return null;
    }

    /**
     * If n is an array length field access, then the type of a.length is the glb
     * of @LTEqLengthOf("a") and the value of a.length in the store. This is case 19.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, CFStore> in) {
        if (NodeUtils.isArrayLengthFieldAccess(n)) {
            FieldAccess arrayLength = JavaExpression.fromNodeFieldAccess(n);
            JavaExpression arrayJe = arrayLength.getReceiver();
            Tree arrayTree = n.getReceiver().getTree();
            TransferResult<CFValue, CFStore> result = visitLengthAccess(n, in, arrayJe, arrayTree);
            if (result != null) {
                return result;
            }
        }
        return super.visitFieldAccess(n, in);
    }

    /**
     * If n is a String.length() method invocation, then the type of s.length() is the glb
     * of @LTEqLengthOf("s") and the value of s.length() in the store. This is case 20.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {

        if (atypeFactory.getMethodIdentifier().isLengthOfMethodInvocation(n)) {
            JavaExpression stringLength = JavaExpression.fromNode(n);
            if (stringLength instanceof MethodCall) {
                JavaExpression receiverJe = ((MethodCall) stringLength).getReceiver();
                Tree receiverTree = n.getTarget().getReceiver().getTree();
                // receiverTree is null when the receiver is implicit "this".
                if (receiverTree != null) {
                    TransferResult<CFValue, CFStore> result =
                            visitLengthAccess(n, in, receiverJe, receiverTree);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return super.visitMethodInvocation(n, in);
    }

    /**
     * Returns the UBQualifier for a node, with additional refinement useful specifically for
     * integer addition, based on the information from subcheckers of the Index Checker.
     *
     * @param n the node
     * @param in dataflow analysis transfer input
     * @return the UBQualifier for {@code node}
     */
    private UBQualifier getUBQualifierForAddition(Node n, TransferInput<CFValue, CFStore> in) {

        // The method takes the greatest lower bound of the qualifier returned by
        // getUBQualifier and a qualifier created from a SubstringIndexFor annotation, if such
        // annotation is present and the index is known to be non-negative.

        UBQualifier ubQualifier = getUBQualifier(n, in);
        Tree nodeTree = n.getTree();
        // Annotation from the Substring Index hierarchy
        AnnotatedTypeMirror substringIndexType =
                atypeFactory.getSubstringIndexAnnotatedTypeFactory().getAnnotatedType(nodeTree);
        AnnotationMirror substringIndexAnno =
                substringIndexType.getAnnotation(SubstringIndexFor.class);
        // Annotation from the Lower bound hierarchy
        AnnotatedTypeMirror lowerBoundType =
                atypeFactory.getLowerBoundAnnotatedTypeFactory().getAnnotatedType(nodeTree);
        // If the index has an SubstringIndexFor annotation and at the same time is non-negative,
        // convert the SubstringIndexFor annotation to a upper bound qualifier.
        if (substringIndexAnno != null
                && (lowerBoundType.hasAnnotation(NonNegative.class)
                        || lowerBoundType.hasAnnotation(Positive.class))) {
            UBQualifier substringIndexQualifier =
                    UBQualifier.createUBQualifier(
                            substringIndexAnno,
                            atypeFactory,
                            atypeFactory.substringIndexAtypeFactory);
            ubQualifier = ubQualifier.glb(substringIndexQualifier);
        }
        return ubQualifier;
    }

    /**
     * Returns the UBQualifier for node. It does this by finding a {@link CFValue} for node. First
     * it checks the store in the transfer input. If one isn't there, the analysis is checked. If
     * the UNKNOWN qualifier is returned, then the AnnotatedTypeMirror from the type factory is
     * used.
     *
     * @param n node
     * @param in transfer input
     * @return the UBQualifier for node
     */
    private UBQualifier getUBQualifier(Node n, TransferInput<CFValue, CFStore> in) {
        QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
        JavaExpression je = JavaExpression.fromNode(n);
        CFValue value = null;
        if (CFAbstractStore.canInsertJavaExpression(je)) {
            value = in.getRegularStore().getValue(je);
        }
        if (value == null) {
            value = analysis.getValue(n);
        }
        UBQualifier qualifier = getUBQualifier(hierarchy, value);
        if (qualifier.isUnknown()) {
            // The qualifier from the store or analysis might be UNKNOWN if there was some error.
            //  For example,
            // @LTLength("a") int i = 4;  // error
            // The type of i in the store is @UpperBoundUnknown, but the type of i as computed by
            // the type factory is @LTLength("a"), so use that type.
            CFValue valueFromFactory = getValueFromFactory(n.getTree(), n);
            return getUBQualifier(hierarchy, valueFromFactory);
        }
        return qualifier;
    }

    private UBQualifier getUBQualifier(QualifierHierarchy hierarchy, CFValue value) {
        if (value == null) {
            return UpperBoundUnknownQualifier.UNKNOWN;
        }
        Set<AnnotationMirror> set = value.getAnnotations();
        AnnotationMirror anno = hierarchy.findAnnotationInHierarchy(set, atypeFactory.UNKNOWN);
        if (anno == null) {
            return UpperBoundUnknownQualifier.UNKNOWN;
        }
        return UBQualifier.createUBQualifier(
                anno, atypeFactory, atypeFactory.substringIndexAtypeFactory);
    }

    private TransferResult<CFValue, CFStore> createTransferResult(
            Node n, TransferInput<CFValue, CFStore> in, UBQualifier qualifier) {
        AnnotationMirror newAnno = atypeFactory.convertUBQualifierToAnnotation(qualifier);
        CFValue value = analysis.createSingleAnnotationValue(newAnno, n.getType());
        return createTransferResult(value, in);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitCase(
            CaseNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitCase(n, in);
        // Refines subtrahend in the switch expression
        // TODO: this cannot be done in strengthenAnnotationOfEqualTo, because that does not provide
        // transfer input
        Node caseNode = n.getCaseOperand();
        AssignmentNode assign = (AssignmentNode) n.getSwitchOperand();
        Node switchNode = assign.getExpression();
        refineSubtrahendWithOffset(switchNode, caseNode, false, in, result.getThenStore());
        return result;
    }
}
