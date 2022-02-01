package org.checkerframework.dataflow.livevariable;

import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;

import java.util.List;

/** A live variable transfer function. */
public class LiveVarTransfer
        extends AbstractNodeVisitor<
                TransferResult<LiveVarValue, LiveVarStore>,
                TransferInput<LiveVarValue, LiveVarStore>>
        implements BackwardTransferFunction<LiveVarValue, LiveVarStore> {

    @Override
    public LiveVarStore initialNormalExitStore(
            UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
        return new LiveVarStore();
    }

    @Override
    public LiveVarStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
        return new LiveVarStore();
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitNode(
            Node n, TransferInput<LiveVarValue, LiveVarStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitAssignment(
            AssignmentNode n, TransferInput<LiveVarValue, LiveVarStore> p) {
        RegularTransferResult<LiveVarValue, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVarValue, LiveVarStore>) super.visitAssignment(n, p);
        processLiveVarInAssignment(
                n.getTarget(), n.getExpression(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<LiveVarValue, LiveVarStore> p) {
        RegularTransferResult<LiveVarValue, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVarValue, LiveVarStore>)
                        super.visitStringConcatenateAssignment(n, p);
        processLiveVarInAssignment(
                n.getLeftOperand(), n.getRightOperand(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<LiveVarValue, LiveVarStore> p) {
        RegularTransferResult<LiveVarValue, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVarValue, LiveVarStore>)
                        super.visitMethodInvocation(n, p);
        LiveVarStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitObjectCreation(
            ObjectCreationNode n, TransferInput<LiveVarValue, LiveVarStore> p) {
        RegularTransferResult<LiveVarValue, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVarValue, LiveVarStore>) super.visitObjectCreation(n, p);
        LiveVarStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVarValue, LiveVarStore> visitReturn(
            ReturnNode n, TransferInput<LiveVarValue, LiveVarStore> p) {
        RegularTransferResult<LiveVarValue, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVarValue, LiveVarStore>) super.visitReturn(n, p);
        Node result = n.getResult();
        if (result != null) {
            LiveVarStore store = transferResult.getRegularStore();
            store.addUseInExpression(result);
        }
        return transferResult;
    }

    /**
     * Update the information of live variables from an assignment statement.
     *
     * @param variable the variable that should be killed
     * @param expression the expression in which the variables should be added
     * @param store the live variable store
     */
    private void processLiveVarInAssignment(Node variable, Node expression, LiveVarStore store) {
        store.killLiveVar(new LiveVarValue(variable));
        store.addUseInExpression(expression);
    }
}
