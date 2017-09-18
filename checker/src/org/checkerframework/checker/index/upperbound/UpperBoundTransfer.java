package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.checker.index.upperbound.UBQualifier.UpperBoundUnknownQualifier;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.MethodCall;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;

public class UpperBoundTransfer extends IndexAbstractTransfer {

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    // Refine the type of expressions used as an array dimension to be
    // less than length of the array to which the new array is assigned.
    // For example, in "int[] array = new int[expr];", the type of expr is @LTEqLength("array").
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
            Receiver arrayRec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());
            String arrayString = arrayRec.toString();
            LessThanLengthOf newInfo =
                    (LessThanLengthOf) UBQualifier.createUBQualifier(arrayString, "-1");
            UBQualifier combined = previousQualifier.glb(newInfo);
            AnnotationMirror newAnno = atypeFactory.convertUBQualifierToAnnotation(combined);

            Receiver dimRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            result.getRegularStore().insertValue(dimRec, newAnno);
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
            if (atypeFactory.hasLowerBoundTypeByClass(node, NonNegative.class)
                    || atypeFactory.hasLowerBoundTypeByClass(node, Positive.class)) {
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
     * typeOfMultiplication} plus 1.
     */
    private void propagateToMultiplicationOperand(
            LessThanLengthOf typeOfMultiplication,
            Node node,
            Node other,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        if (atypeFactory.hasLowerBoundTypeByClass(other, Positive.class)) {
            Long minValue =
                    IndexUtil.getMinValue(
                            other.getTree(), atypeFactory.getValueAnnotatedTypeFactory());
            if (minValue != null && minValue > 1) {
                typeOfMultiplication = (LessThanLengthOf) typeOfMultiplication.plusOffset(1);
            }
            UBQualifier qual = getUBQualifier(node, in);
            UBQualifier newQual = qual.glb(typeOfMultiplication);
            Receiver rec = FlowExpressions.internalReprOf(atypeFactory, node);
            store.insertValue(rec, atypeFactory.convertUBQualifierToAnnotation(newQual));
        }
    }

    /**
     * The subtraction node, {@code node}, is known to be {@code typeOfSubtraction}.
     *
     * <p>This means that the left node is less than or equal to the length of the array when the
     * right node is subtracted from the left node. Note that unlike {@link
     * #propagateToAdditionOperand(LessThanLengthOf, Node, Node, TransferInput, CFStore)} and {@link
     * #propagateToMultiplicationOperand(LessThanLengthOf, Node, Node, TransferInput, CFStore)},
     * this method takes the NumericalSubtractionNode instead of the two operand nodes.
     *
     * @param typeOfSubtraction type of node
     * @param node subtraction node that has typeOfSubtraction
     * @param in TransferInput
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
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, node.getLeftOperand());
        store.insertValue(leftRec, atypeFactory.convertUBQualifierToAnnotation(newLeft));
    }

    /**
     * Refines the type of {@code operand} to {@code typeOfAddition} plus {@code other}. If {@code
     * other} is non-negative, then {@code operand} also less than the length of the arrays in
     * {@code typeOfAddition}. If {@code other} is positive, then {@code operand} also less than the
     * length of the arrays in {@code typeOfAddition} plus 1.
     *
     * @param typeOfAddition type of {@code operand + other}
     * @param operand the Node to refine
     * @param other the Node added to {@code operand}
     * @param in TransferInput
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
        Receiver operandRec = FlowExpressions.internalReprOf(atypeFactory, operand);
        store.insertValue(operandRec, atypeFactory.convertUBQualifierToAnnotation(newQual));
    }

    @Override
    protected void refineGT(
            Node larger,
            AnnotationMirror largerAnno,
            Node smaller,
            AnnotationMirror smallerAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        // larger > smaller
        UBQualifier largerQual = UBQualifier.createUBQualifier(largerAnno);
        // larger + 1 >= smaller
        UBQualifier largerQualPlus1 = largerQual.plusOffset(1);
        UBQualifier rightQualifier = UBQualifier.createUBQualifier(smallerAnno);
        UBQualifier refinedRight = rightQualifier.glb(largerQualPlus1);

        if (largerQualPlus1.isLessThanLengthQualifier()) {
            propagateToOperands((LessThanLengthOf) largerQualPlus1, smaller, in, store);
        }

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), smaller);
        store.insertValue(rightRec, atypeFactory.convertUBQualifierToAnnotation(refinedRight));
    }

    /**
     * This method refines the type of the right expression to the glb the previous type of right
     * and the type of left.
     *
     * <p>Also, if the left expression is an array access, then the types of sub expressions of the
     * right are refined.
     */
    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        UBQualifier leftQualifier = UBQualifier.createUBQualifier(leftAnno);
        UBQualifier rightQualifier = UBQualifier.createUBQualifier(rightAnno);
        UBQualifier refinedRight = rightQualifier.glb(leftQualifier);

        if (leftQualifier.isLessThanLengthQualifier()) {
            propagateToOperands((LessThanLengthOf) leftQualifier, right, in, store);
        }

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        store.insertValue(rightRec, atypeFactory.convertUBQualifierToAnnotation(refinedRight));
    }

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
        UBQualifier leftQualifier = UBQualifier.createUBQualifier(leftAnno);
        UBQualifier rightQualifier = UBQualifier.createUBQualifier(rightAnno);
        UBQualifier glb = rightQualifier.glb(leftQualifier);
        AnnotationMirror glbAnno = atypeFactory.convertUBQualifierToAnnotation(glb);

        List<Node> internalsRight = splitAssignments(right);
        for (Node internal : internalsRight) {
            Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal);
            store.insertValue(rightRec, glbAnno);
        }

        List<Node> internalsLeft = splitAssignments(left);
        for (Node internal : internalsLeft) {
            Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal);
            store.insertValue(leftRec, glbAnno);
        }
    }

    /**
     * If lengthAccess node is an sequence length field or method access (optionally with a constant
     * offset subtracted) and the other node is less than or equal to that sequence length (minus
     * the offset), then refine the other nodes type to less than the sequence length (minus the
     * offset).
     */
    private void refineNeqSequenceLength(
            Node lengthAccess, Node otherNode, AnnotationMirror otherNodeAnno, CFStore store) {

        Receiver receiver = null;
        // If lengthAccess is "receiver.length - c" where c is an integer constant, stores c
        // into lengthOffset
        int lengthOffset = 0;
        if (lengthAccess instanceof NumericalSubtractionNode) {
            NumericalSubtractionNode subtraction = (NumericalSubtractionNode) lengthAccess;
            Node offsetNode = subtraction.getRightOperand();
            Long offsetValue =
                    IndexUtil.getExactValue(
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

        if (NodeUtils.isArrayLengthFieldAccess(lengthAccess)) {
            FieldAccess fa =
                    FlowExpressions.internalReprOfFieldAccess(
                            atypeFactory, (FieldAccessNode) lengthAccess);
            receiver = fa.getReceiver();

        } else if (atypeFactory.getMethodIdentifier().isStringLengthInvocation(lengthAccess)) {
            Receiver ma = FlowExpressions.internalReprOf(atypeFactory, lengthAccess);
            if (ma instanceof MethodCall) {
                receiver = ((MethodCall) ma).getReceiver();
            }
        }

        if (receiver != null && !receiver.containsUnknown()) {
            UBQualifier otherQualifier = UBQualifier.createUBQualifier(otherNodeAnno);
            String sequence = receiver.toString();
            // Check if otherNode + c - 1 < receiver.length
            if (otherQualifier.hasSequenceWithOffset(sequence, lengthOffset - 1)) {
                // Add otherNode + c < receiver.length
                UBQualifier newQualifier =
                        UBQualifier.createUBQualifier(sequence, Integer.toString(lengthOffset));
                otherQualifier = otherQualifier.glb(newQualifier);
                for (Node internal : splitAssignments(otherNode)) {
                    Receiver leftRec =
                            FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal);
                    store.insertValue(
                            leftRec, atypeFactory.convertUBQualifierToAnnotation(otherQualifier));
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
     */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(
            NumericalAdditionNode n, TransferInput<CFValue, CFStore> in) {
        // type of leftNode + rightNode  is  glb(T, S) where
        // T = minusOffset(type(leftNode), rightNode) and
        // S = minusOffset(type(rightNode), leftNode)

        UBQualifier left = getUBQualifier(n.getLeftOperand(), in);
        UBQualifier T = left.minusOffset(n.getRightOperand(), atypeFactory);

        UBQualifier right = getUBQualifier(n.getRightOperand(), in);
        UBQualifier S = right.minusOffset(n.getLeftOperand(), atypeFactory);

        UBQualifier glb = T.glb(S);
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
     * Return the result of adding i to j, when expression i has type @LTLengthOf(value = "f2",
     * offset = "f1.length") int and expression j is less than or equal to the length of f1, then
     * the type of i + j is @LTLengthOf("f2").
     *
     * <p>Similarly, return the result of adding i to j, when expression i has type @LTLengthOf
     * (value = "f2", offset = "f1.length - 1") int and expression j is less than the length of f1,
     * then the type of i + j is @LTLengthOf("f2").
     *
     * @param i the type of the expression added to j
     * @param j the type of the expression added to i
     * @return the type of i + j
     */
    private UBQualifier removeSequenceLengths(LessThanLengthOf i, LessThanLengthOf j) {
        List<String> lessThan = new ArrayList<>();
        List<String> lessThanOrEqaul = new ArrayList<>();
        for (String sequence : i.getSequences()) {
            if (i.isLessThanLengthOf(sequence)) {
                lessThan.add(sequence);
            } else if (i.hasSequenceWithOffset(sequence, -1)) {
                lessThanOrEqaul.add(sequence);
            }
        }
        // Creates a qualifier that is the same a j with the array.length offsets removed. If
        // an offset doesn't have an array.length, then the offset/array pair is removed. If
        // there are no such pairs, Unknown is returned.
        UBQualifier lessThanEqQ = j.removeSequenceLengthAccess(lessThanOrEqaul);
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
     * or positive, then a - b should keep the types of a.
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
        return createTransferResult(n, in, leftWithOffset);
    }

    /**
     * Computes a type of a sequence length access.
     *
     * @param n sequence length access node
     */
    private TransferResult<CFValue, CFStore> visitLengthAccess(
            Node n, TransferInput<CFValue, CFStore> in, Receiver sequenceRec, Tree sequenceTree) {
        // Look up the SameLen type of the sequence.
        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromTree(sequenceTree);
        List<String> sameLenSequences =
                sameLenAnno == null
                        ? new ArrayList<String>()
                        : IndexUtil.getValueOfAnnotationWithStringArgument(sameLenAnno);

        if (!sameLenSequences.contains(sequenceRec.toString())) {
            sameLenSequences.add(sequenceRec.toString());
        }

        ArrayList<String> offsets = new ArrayList<>(sameLenSequences.size());
        for (String s : sameLenSequences) {
            offsets.add("-1");
        }

        if (CFAbstractStore.canInsertReceiver(sequenceRec)) {
            UBQualifier qualifier = UBQualifier.createUBQualifier(sameLenSequences, offsets);
            UBQualifier previous = getUBQualifier(n, in);
            return createTransferResult(n, in, qualifier.glb(previous));
        }

        return null;
    }

    /**
     * If n is an array length field access, then the type of a.length is the glb
     * of @LTEqLengthOf("a") and the value of a.length in the store.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, CFStore> in) {
        if (NodeUtils.isArrayLengthFieldAccess(n)) {
            FieldAccess arrayLength = FlowExpressions.internalReprOfFieldAccess(atypeFactory, n);
            Receiver arrayRec = arrayLength.getReceiver();
            Tree arrayTree = n.getReceiver().getTree();
            TransferResult<CFValue, CFStore> result = visitLengthAccess(n, in, arrayRec, arrayTree);
            if (result != null) {
                return result;
            }
        }
        return super.visitFieldAccess(n, in);
    }

    /**
     * If n is an String.length() method invocation, then the type of s.length() is the glb
     * of @LTEqLengthOf("s") and the value of s.length() in the store.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {

        if (atypeFactory.getMethodIdentifier().isStringLengthInvocation(n)) {
            Receiver stringLength = FlowExpressions.internalReprOf(atypeFactory, n);
            if (stringLength instanceof MethodCall) {
                Receiver stringRec = ((MethodCall) stringLength).getReceiver();
                Tree stringTree = n.getTarget().getReceiver().getTree();
                TransferResult<CFValue, CFStore> result =
                        visitLengthAccess(n, in, stringRec, stringTree);
                if (result != null) {
                    return result;
                }
            }
        }
        return super.visitMethodInvocation(n, in);
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
        Receiver rec = FlowExpressions.internalReprOf(atypeFactory, n);
        CFValue value = null;
        if (CFAbstractStore.canInsertReceiver(rec)) {
            value = in.getRegularStore().getValue(rec);
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
        return UBQualifier.createUBQualifier(anno);
    }

    private TransferResult<CFValue, CFStore> createTransferResult(
            Node n, TransferInput<CFValue, CFStore> in, UBQualifier qualifier) {
        AnnotationMirror newAnno = atypeFactory.convertUBQualifierToAnnotation(qualifier);
        CFValue value = analysis.createSingleAnnotationValue(newAnno, n.getType());
        if (in.containsTwoStores()) {
            CFStore thenStore = in.getThenStore();
            CFStore elseStore = in.getElseStore();
            return new ConditionalTransferResult<>(
                    finishValue(value, thenStore, elseStore), thenStore, elseStore);
        } else {
            CFStore info = in.getRegularStore();
            return new RegularTransferResult<>(finishValue(value, info), info);
        }
    }
}
