package org.checkerframework.checker.index.upperbound;

import javax.lang.model.type.TypeKind;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;

public class UpperBoundStore extends CFAbstractStore<CFValue, UpperBoundStore> {

    public UpperBoundStore(UpperBoundAnalysis analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    public UpperBoundStore(
            UpperBoundAnalysis analysis, CFAbstractStore<CFValue, UpperBoundStore> other) {
        super(other);
    }

    @Override
    public void updateForAssignment(Node n, CFValue val) {
        // Do reassignment things here.

        System.out.println(n);
        System.out.println(n.getClass());

        if (n instanceof AssignmentNode) {
            AssignmentNode node = (AssignmentNode) n;
            Node target = node.getTarget();
            if (target.getType().getKind() == TypeKind.ARRAY) {
                System.out.println("array reassignment: " + node);
                FlowExpressions.Receiver targetRec =
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), target);
                String canonicalTargetName = targetRec.toString();
                System.out.println("targetName: " + canonicalTargetName);
            }
        }

        super.updateForAssignment(n, val);
    }
}
