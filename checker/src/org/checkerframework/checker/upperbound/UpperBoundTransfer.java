package org.checkerframework.checker.upperbound;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

public class UpperBoundTransfer extends CFAbstractTransfer<CFValue, CFStore, UpperBoundTransfer> {
    protected UpperBoundAnalysis analysis;

    private final AnnotationMirror LTL, EL, LTEL, UNKNOWN;

    private UpperBoundAnnotatedTypeFactory atypeFactory;

    public UpperBoundTransfer(UpperBoundAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (UpperBoundAnnotatedTypeFactory) analysis.getTypeFactory();
        LTL = UpperBoundAnnotatedTypeFactory.LTL;
        EL = UpperBoundAnnotatedTypeFactory.EL;
        LTEL = UpperBoundAnnotatedTypeFactory.LTEL;
        UNKNOWN = UpperBoundAnnotatedTypeFactory.UNKNOWN;
    }

    // Make variables used in array creation have reasonable types.
    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            AssignmentNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);
        if (node.getExpression() instanceof ArrayCreationNode) {
            ArrayCreationNode acNode = (ArrayCreationNode) node.getExpression();
            CFStore store = result.getRegularStore();
            List<Node> nodeList = acNode.getDimensions();
            // The dimenions list is empty -> dimensions aren't known, I believe.
            if (nodeList.size() < 1) {
                return result;
            }
            Node dim = acNode.getDimension(0);
            Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), dim);
            String name = node.getTarget().toString();

            // FIXME: the Index Checker includes this here. Not sure why - investigate.
            // if (dim instanceof NumericalAdditionNode) {
            //     if (isVarPlusOne((NumericalAdditionNode)dim, store, name)) {
            //         return result;
            //     }
            // }
            String[] names = {name};

            store.insertValue(
                    rec, UpperBoundAnnotatedTypeFactory.createLessThanLengthAnnotation(names));
        }
        return result;
    }

    // Make array.length have type EL(array).
    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode node, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitFieldAccess(node, in);
        if (node.getFieldName().equals("length")) {
            // I'm concerned about the level of evil present in this code.
            // It's modeled on similar code in the old Index Checker, and it feels like a bad
            // way to do this, but I don't know a better way.
            String arrName = node.getReceiver().toString();
            AnnotationMirror anm =
                    UpperBoundAnnotatedTypeFactory.createEqualToLengthAnnotation(arrName);
            CFValue newResultValue =
                    analysis.createSingleAnnotationValue(
                            anm, result.getResultValue().getType().getUnderlyingType());
            return new RegularTransferResult<>(newResultValue, result.getRegularStore());
        }
        return result;
    }
}
