package org.checkerframework.checker.minlen;

import com.sun.source.tree.Tree;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class MinLenTransfer extends CFAbstractTransfer<MinLenValue, MinLenStore, MinLenTransfer> {

    protected MinLenAnalysis analysis;
    protected static MinLenAnnotatedTypeFactory atypeFactory;
    protected final ProcessingEnvironment env;
    protected final ExecutableElement listAdd;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.env = MinLenAnnotatedTypeFactory.env;
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
    }

    /**
     * This struct contains all of the information that the refinement functions need. It's called
     * by each node function (i.e. greater than node, less than node, etc.) and then the results are
     * passed to the refinement function in whatever order is appropriate for that node. Its
     * constructor contains all of its logic. I originally wrote this for LowerBoundTransfer but I'm
     * duplicating it here since I need it again...maybe it should live elsewhere and be shared? I
     * don't know where though.
     */
    private class RefinementInfo {
        public Node left, right;
        public Set<AnnotationMirror> leftType, rightType;
        public MinLenStore thenStore, elseStore;
        public ConditionalTransferResult<MinLenValue, MinLenStore> newResult;

        public RefinementInfo(
                TransferResult<MinLenValue, MinLenStore> result,
                TransferInput<MinLenValue, MinLenStore> in,
                Node r,
                Node l) {
            right = r;
            left = l;

            rightType = in.getValueOfSubNode(right).getAnnotations();
            leftType = in.getValueOfSubNode(left).getAnnotations();

            thenStore = result.getRegularStore();
            elseStore = thenStore.copy();

            newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
        }
    }

    // So I actually just ended up copying these from Lower Bound Transfer too.
    // The only parts that are actually different are the definitions of
    // refineGT and refineGTE, and the handling of equals and not equals. The
    // code for the visitGreaterThan, visitLessThan, etc., are all identical to
    // their LBC counterparts.

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitGreaterThan(
            GreaterThanNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitGreaterThan(node, in);
        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch, which is the inverse of the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitGreaterThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitLessThanOrEqual(
            LessThanOrEqualNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitLessThanOrEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A <= is just a flipped >=.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGT(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitLessThan(
            LessThanNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitLessThan(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch. A < is just a flipped >.
        refineGT(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);

        // Refine the else branch.
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);
        return rfi.newResult;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitEqualTo(
            EqualToNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitEqualTo(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        // The else branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineZeroEquality(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineZeroEquality(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        return rfi.newResult;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitNotEqual(
            NotEqualNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitNotEqual(node, in);

        RefinementInfo rfi =
                new RefinementInfo(result, in, node.getRightOperand(), node.getLeftOperand());

        // Refine the then branch.
        refineGTE(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.elseStore);
        refineGTE(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.elseStore);

        // The then branch should only be refined if a length is being compared
        // to zero. The following code block implements this special case.
        // This special case occurs because zero is a hard bound on the bottom
        // of the array (i.e. no array can be smaller than zero), so in this
        // case the MinLen of the array is one.
        refineZeroEquality(rfi.right, rfi.rightType, rfi.left, rfi.leftType, rfi.thenStore);
        refineZeroEquality(rfi.left, rfi.leftType, rfi.right, rfi.rightType, rfi.thenStore);

        return rfi.newResult;
    }

    private void refineZeroEquality(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // We only care about length. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.

        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }
        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to. If so, we can do something.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.minLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            // We must be comparing against zero here; otherwise, we should be using
            // refineGTE.
            if (newMinLen != 0) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }

            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);

            if (1 > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(1));
                return;
            }

            return;
        }
    }

    private void refineGT(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // We only care about length. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.

        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }

        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to. If so, we can do something.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.minLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }

            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);

            if (newMinLen + 1 > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(newMinLen + 1));
                return;
            }

            return;
        }
    }

    private void refineGTE(
            Node left,
            Set<AnnotationMirror> leftType,
            Node right,
            Set<AnnotationMirror> rightType,
            MinLenStore store) {
        FieldAccessNode fi = null;
        Tree tree = null;
        Receiver rec = null;
        Set<AnnotationMirror> type = null;
        // We only care about length. This will miss an expression which
        // include an array length (like "a.length + 1"), but that's okay
        // for now.
        // FIXME: Joe: List support will be needed here too.
        if (left instanceof FieldAccessNode) {
            fi = (FieldAccessNode) left;
            tree = right.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = leftType;
        } else if (right instanceof FieldAccessNode) {
            fi = (FieldAccessNode) right;
            tree = left.getTree();
            rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), fi.getReceiver());
            type = rightType;
        } else {
            return;
        }

        if (fi == null || tree == null || rec == null || type == null) {
            return;
        }

        if (fi.getFieldName().equals("length")
                && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
            // At this point, MinLen needs to invoke the constant value checker
            // to find out if it knows anything about what the length is being
            // compared to. If so, we can do something.

            AnnotatedTypeMirror valueType = atypeFactory.valueTypeFromTree(tree);

            if (valueType == null) {
                return;
            }

            Integer newMinLen = atypeFactory.minLenFromValueType(valueType);

            if (newMinLen == null) {
                return;
            }

            AnnotationMirror anno = AnnotationUtils.getAnnotationByClass(type, MinLen.class);
            if (!AnnotationUtils.hasElementValue(anno, "value")) {
                return;
            }
            Integer currentMinLen =
                    AnnotationUtils.getElementValue(anno, "value", Integer.class, true);
            if (newMinLen > currentMinLen) {
                store.insertValue(rec, atypeFactory.createMinLen(newMinLen));
                return;
            }

            return;
        }
    }
}
