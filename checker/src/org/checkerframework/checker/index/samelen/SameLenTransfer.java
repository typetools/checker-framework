package org.checkerframework.checker.index.samelen;

import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The transfer function for the SameLen checker. Contains three cases:
 *
 * <ol>
 *   <li>"b = new T[a.length]" implies that b is the same length as a.
 *   <li>after "if (a.length == b.length)", a and b have the same length.
 *   <li>after "if (a == b)", a and b have the same length, if they are arrays.
 * </ol>
 */
public class SameLenTransfer extends CFTransfer {

    // The ATF (Annotated Type Factory).
    private SameLenAnnotatedTypeFactory aTypeFactory;

    /** Shorthand for SameLenUnknown.class. */
    private AnnotationMirror UNKNOWN;

    private CFAnalysis analysis;

    public SameLenTransfer(CFAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        aTypeFactory = (SameLenAnnotatedTypeFactory) analysis.getTypeFactory();
        UNKNOWN = aTypeFactory.UNKNOWN;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

        System.out.println("visiting this assignment: " + node);

        // Handle b = new T[a.length]
        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            if (acNode.getDimensions().size() == 1 && isArrayLengthAccess(acNode.getDimension(0))) {
                // "new T[a.length]" is the right hand side of the assignment.
                FieldAccessNode arrayLengthNode = (FieldAccessNode) acNode.getDimension(0);
                Node arrayLengthNodeReceiver = arrayLengthNode.getReceiver();
                // arrayLengthNode is known to be "new T[arrayLengthNodeReceiver.length]"

                // targetRec is the receiver for the left hand side of the assignment.
                Receiver targetRec =
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());
                Receiver otherRec =
                        FlowExpressions.internalReprOf(
                                analysis.getTypeFactory(), arrayLengthNodeReceiver);

                AnnotationMirror arrayLengthNodeAnnotation =
                        aTypeFactory
                                .getAnnotatedType(arrayLengthNodeReceiver.getTree())
                                .getAnnotationInHierarchy(UNKNOWN);

                AnnotationMirror combinedSameLen =
                        aTypeFactory.createCombinedSameLen(
                                targetRec, otherRec, UNKNOWN, arrayLengthNodeAnnotation);

                propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
                return result;
            }
        }

        AnnotationMirror rightAnno =
                aTypeFactory
                        .getAnnotatedType(node.getExpression().getTree())
                        .getAnnotationInHierarchy(UNKNOWN);

        // If the left side of the assignment is an array, then have both the right and left side be SameLen
        // of each other.

        Receiver targetRec =
                FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget());

        Receiver exprRec =
                FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getExpression());

        if (node.getTarget().getType().getKind() == TypeKind.ARRAY
                || (rightAnno != null
                        && AnnotationUtils.areSameByClass(rightAnno, SameLen.class))) {

            AnnotationMirror rightAnnoOrUnknown = rightAnno == null ? UNKNOWN : rightAnno;

            AnnotationMirror combinedSameLen =
                    aTypeFactory.createCombinedSameLen(
                            targetRec, exprRec, UNKNOWN, rightAnnoOrUnknown);

            propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
        }

        return result;
    }

    /**
     * Insert combinedSameLen into the store as the SameLen type of each array listed in
     * combinedSameLen.
     *
     * @param combinedSameLen A Samelen annotation. Not just an annotation in the SameLen hierarchy;
     *     this annotation MUST be @SameLen().
     * @param node The node in the tree where the combination is happening. Used for context.
     * @param store The store to modify
     */
    private void propagateCombinedSameLen(
            AnnotationMirror combinedSameLen, Node node, CFStore store) {
        TreePath currentPath = aTypeFactory.getPath(node.getTree());
        if (currentPath == null) {
            return;
        }
        for (String s : IndexUtil.getValueOfAnnotationWithStringArgument(combinedSameLen)) {
            Receiver recS;
            try {
                recS = aTypeFactory.getReceiverFromJavaExpressionString(s, currentPath);
            } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                continue;
            }
            store.clearValue(recS);
            store.insertValue(recS, combinedSameLen);
        }
    }

    /** Returns true if node is of the form "someArray.length". */
    private boolean isArrayLengthAccess(Node node) {
        return (node instanceof FieldAccessNode
                && ((FieldAccessNode) node).getFieldName().equals("length")
                && ((FieldAccessNode) node).getReceiver().getType().getKind() == TypeKind.ARRAY);
    }
    /**
     * Handles refinement of equality comparisons. After "a.length == b.length" evaluates to true, a
     * and b have SameLen of each other in the store.
     */
    private void refineEq(Node left, Node right, CFStore store) {
        Receiver leftRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), left);
        Receiver rightRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), right);

        AnnotationMirror leftAnno = getAnno(left);
        AnnotationMirror rightAnno = getAnno(right);
        AnnotationMirror combinedSameLen =
                aTypeFactory.createCombinedSameLen(leftRec, rightRec, leftAnno, rightAnno);

        propagateCombinedSameLen(combinedSameLen, left, store);
    }

    /** Return n's annotation from the SameLen hierarchy. */
    AnnotationMirror getAnno(Node n) {
        CFValue cfValue = analysis.getValue(n);
        if (cfValue == null) {
            return UNKNOWN;
        }
        if (cfValue.getAnnotations().size() == 1) {
            return cfValue.getAnnotations().iterator().next();
        }
        return UNKNOWN;
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
        CFStore equalStore = notEqualTo ? result.getElseStore() : result.getThenStore();
        if (isArrayLengthAccess(firstNode) && isArrayLengthAccess(secondNode)) {
            // Refinement in the else store if this is a.length == b.length.
            refineEq(
                    ((FieldAccessNode) firstNode).getReceiver(),
                    ((FieldAccessNode) secondNode).getReceiver(),
                    equalStore);
        } else if (firstNode.getType().getKind() == TypeKind.ARRAY
                || secondNode.getType().getKind() == TypeKind.ARRAY) {
            refineEq(firstNode, secondNode, equalStore);
        }

        return new ConditionalTransferResult<>(
                result.getResultValue(), result.getThenStore(), result.getElseStore());
    }
}
