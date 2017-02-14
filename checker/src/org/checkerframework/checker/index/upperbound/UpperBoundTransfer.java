package org.checkerframework.checker.index.upperbound;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.checker.index.IndexRefinementInfo;
import org.checkerframework.checker.index.upperbound.UBQualifier.UpperBoundUnknownQualifier;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.FieldAccess;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
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
    // less than length of the array to which the new array is
    // assigned.  For example int[] array = new int[expr]; the type of expr is @LTEqLength("array")
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            List<Node> nodeList = acNode.getDimensions();
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);

            UBQualifier previousQualifier = getUBQualifier(dim, in);
            Receiver arrayRec =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());
            String arrayString = arrayRec.toString();
            UBQualifier newInfo = UBQualifier.createUBQualifier(arrayString, "-1");
            UBQualifier combined = previousQualifier.glb(newInfo);
            AnnotationMirror newAnno = atypeFactory.convertUBQualifierToAnnotation(combined);

            Receiver dimRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            result.getRegularStore().insertValue(dimRec, newAnno);
            knownToBeLessThanLengthOf(arrayString, dim, result.getRegularStore(), in);
        }
        return result;
    }

    /**
     * Node is known to be less than the length of array. If the node is a plus or a minus then the
     * types of the left and right operands can be refined to include offsets.
     */
    private void knownToBeLessThanLengthOf(
            String array, Node node, CFStore store, TransferInput<CFValue, CFStore> in) {
        if (node instanceof NumericalAdditionNode) {
            knownToBeArrayLength((NumericalAdditionNode) node, array, in, store);
        } else if (node instanceof NumericalSubtractionNode) {
            knownToBeArrayLength((NumericalSubtractionNode) node, array, in, store);
        }
    }

    /**
     * The subtraction node is known to be exactly the length of the array referenced by the
     * arrayExp. This means that the left node is less than or equal to the length of the array when
     * the right node is subtracted from the left node.
     *
     * @param node subtraction node that is known to be equal to the length of the array referenced
     *     by arrayExp
     * @param arrayExp array expression
     * @param in TransferInput
     * @param store location to store the refined type
     */
    private void knownToBeArrayLength(
            NumericalSubtractionNode node,
            String arrayExp,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        UBQualifier newInfo = UBQualifier.createUBQualifier(arrayExp, "-1");
        UBQualifier left = getUBQualifier(node.getLeftOperand(), in);

        UBQualifier newLeft = left.glb(newInfo.minusOffset(node.getRightOperand(), atypeFactory));
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, node.getLeftOperand());
        store.insertValue(leftRec, atypeFactory.convertUBQualifierToAnnotation(newLeft));
    }

    /**
     * The addition node is known to be exactly the length of the array referenced by the arrayExp.
     * This means that the left node is less than or equal to the length of the array when the right
     * node is added to the left node. And this means that the right node is less than or equal to
     * the length of the array when the left node is added to the right node.
     *
     * @param node addition node that is known to be equal to the length of the array referenced by
     *     arrayExp
     * @param arrayExp array expression
     * @param in TransferInput
     * @param store location to store the refined types
     */
    private void knownToBeArrayLength(
            NumericalAdditionNode node,
            String arrayExp,
            TransferInput<CFValue, CFStore> in,
            CFStore store) {
        UBQualifier left = getUBQualifier(node.getLeftOperand(), in);
        UBQualifier right = getUBQualifier(node.getRightOperand(), in);

        UBQualifier newInfo = UBQualifier.createUBQualifier(arrayExp, "-1");
        UBQualifier newLeft = left.glb(newInfo.plusOffset(node.getRightOperand(), atypeFactory));
        Receiver leftRec = FlowExpressions.internalReprOf(atypeFactory, node.getLeftOperand());
        store.insertValue(leftRec, atypeFactory.convertUBQualifierToAnnotation(newLeft));

        UBQualifier newRight = right.glb(newInfo.plusOffset(node.getLeftOperand(), atypeFactory));
        Receiver rightRec = FlowExpressions.internalReprOf(atypeFactory, node.getRightOperand());
        store.insertValue(rightRec, atypeFactory.convertUBQualifierToAnnotation(newRight));
    }

    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        // left > right
        UBQualifier leftQualifier = UBQualifier.createUBQualifier(leftAnno);
        leftQualifier = leftQualifier.plusOffset(1);
        UBQualifier rightQualifier = UBQualifier.createUBQualifier(rightAnno);
        UBQualifier refinedRight = rightQualifier.glb(leftQualifier);

        if (isArrayLengthFieldAccess(left)) {
            String array = ((FieldAccessNode) left).getReceiver().toString();
            knownToBeLessThanLengthOf(array, right, store, in);
        }

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        store.insertValue(rightRec, atypeFactory.convertUBQualifierToAnnotation(refinedRight));
    }

    /**
     *
     *
     * <pre>left >= right</pre>
     *
     * This method refines the type of the right expression to the glb the previous type of right
     * and the type of left.
     *
     * <pre>a.length >= b op c</pre>
     *
     * The type of b is @LTLengthOf("a", offset = "op)
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

        if (isArrayLengthFieldAccess(left)) {
            String array = ((FieldAccessNode) left).getReceiver().toString();
            knownToBeLessThanLengthOf(array, right, store, in);
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

        refineNeqArrayLength(rfi.left, rfi.right, rfi.rightAnno, notEqualStore);
        refineNeqArrayLength(rfi.right, rfi.left, rfi.leftAnno, notEqualStore);
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

        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);
        store.insertValue(rightRec, glbAnno);

        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
        store.insertValue(leftRec, glbAnno);
    }

    /**
     * If arrayLengthAccess node is an array length field access and the other node is less than or
     * equal to that array length, then refine the other nodes type to less than the array length.
     */
    private void refineNeqArrayLength(
            Node arrayLengthAccess, Node otherNode, AnnotationMirror otherNodeAnno, CFStore store) {
        if (isArrayLengthFieldAccess(arrayLengthAccess)) {
            UBQualifier otherQualifier = UBQualifier.createUBQualifier(otherNodeAnno);
            FieldAccess fa =
                    FlowExpressions.internalReprOfFieldAccess(
                            atypeFactory, (FieldAccessNode) arrayLengthAccess);
            if (!fa.getReceiver().containsUnknown()) {
                String array = fa.getReceiver().toString();
                if (otherQualifier.isLessThanOrEqualTo(array)) {
                    otherQualifier = otherQualifier.glb(UBQualifier.createUBQualifier(array, "0"));
                    Receiver leftRec =
                            FlowExpressions.internalReprOf(analysis.getTypeFactory(), otherNode);
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

        return createTransferResult(n, in, glb);
    }

    /**
     * If some Node a is known to be less than the length of some array, x, then, the type of a - b,
     * is @LTLengthOf(value="x", offset="b"). If b is known to be less than the length of some other
     * array, this doesn't add any information about the type of a - b.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> in) {
        UBQualifier left = getUBQualifier(n.getLeftOperand(), in);
        left = left.plusOffset(n.getRightOperand(), atypeFactory);
        return createTransferResult(n, in, left);
    }

    /**
     * If n is an array length field access, then the type of a.length, is the glb
     * of @LTEqLengthOf("a") and the value of a.length in the store.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, CFStore> in) {
        if (isArrayLengthFieldAccess(n)) {
            FieldAccess arrayLength = FlowExpressions.internalReprOfFieldAccess(atypeFactory, n);
            Receiver arrayRec = arrayLength.getReceiver();
            if (CFAbstractStore.canInsertReceiver(arrayRec)) {
                UBQualifier qualifier = UBQualifier.createUBQualifier(arrayRec.toString(), "-1");
                UBQualifier previous = getUBQualifier(n, in);
                return createTransferResult(n, in, qualifier.glb(previous));
            }
        }
        return super.visitFieldAccess(n, in);
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
