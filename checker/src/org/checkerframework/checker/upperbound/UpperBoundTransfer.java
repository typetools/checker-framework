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
            if (name.contains(".")) {
                String[] objs = name.split("\\.");
                name = objs[objs.length - 1];
            }

            // FIXME: the Index Checker includes this here. Not sure why - investigate.
            // if (dim instanceof NumericalAdditionNode) {
            //     if (isVarPlusOne((NumericalAdditionNode)dim, store, name)) {
            //         return result;
            //     }
            // }

            store.insertValue(
                    rec, UpperBoundAnnotatedTypeFactory.createEqualToLengthAnnotation(name));
        }
        return result;
    }
}
