package org.checkerframework.dataflow.busyexpr;

import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

import java.util.List;

/** A busy expression transfer function */
public class BusyExprTransfer
        extends AbstractNodeVisitor<
                TransferResult<UnusedAbstractValue, BusyExprStore>,
                TransferInput<UnusedAbstractValue, BusyExprStore>>
        implements BackwardTransferFunction<UnusedAbstractValue, BusyExprStore> {

    @Override
    public BusyExprStore initialNormalExitStore(
            UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
        return new BusyExprStore();
    }

    @Override
    public BusyExprStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
        return new BusyExprStore();
    }

    @Override
    public RegularTransferResult<UnusedAbstractValue, BusyExprStore> visitNode(
            Node n, TransferInput<UnusedAbstractValue, BusyExprStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<UnusedAbstractValue, BusyExprStore> visitAssignment(
            AssignmentNode n, TransferInput<UnusedAbstractValue, BusyExprStore> p) {
        RegularTransferResult<UnusedAbstractValue, BusyExprStore> transferResult =
                (RegularTransferResult<UnusedAbstractValue, BusyExprStore>)
                        super.visitAssignment(n, p);
        BusyExprStore store = transferResult.getRegularStore();
        store.killBusyExpr(n.getTarget());
        store.addUseInExpression(n.getExpression());
        return transferResult;
    }

    @Override
    public RegularTransferResult<UnusedAbstractValue, BusyExprStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<UnusedAbstractValue, BusyExprStore> p) {
        RegularTransferResult<UnusedAbstractValue, BusyExprStore> transferResult =
                (RegularTransferResult<UnusedAbstractValue, BusyExprStore>)
                        super.visitMethodInvocation(n, p);
        BusyExprStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<UnusedAbstractValue, BusyExprStore> visitObjectCreation(
            ObjectCreationNode n, TransferInput<UnusedAbstractValue, BusyExprStore> p) {
        RegularTransferResult<UnusedAbstractValue, BusyExprStore> transferResult =
                (RegularTransferResult<UnusedAbstractValue, BusyExprStore>)
                        super.visitObjectCreation(n, p);
        BusyExprStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<UnusedAbstractValue, BusyExprStore> visitReturn(
            ReturnNode n, TransferInput<UnusedAbstractValue, BusyExprStore> p) {
        RegularTransferResult<UnusedAbstractValue, BusyExprStore> transferResult =
                (RegularTransferResult<UnusedAbstractValue, BusyExprStore>) super.visitReturn(n, p);
        Node result = n.getResult();
        if (result != null) {
            BusyExprStore store = transferResult.getRegularStore();
            store.addUseInExpression(result);
        }
        return transferResult;
    }
}
