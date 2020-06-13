package org.checkerframework.dataflow.livevariable;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
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

/** A live variable transfer function. */
public class LiveVarTransfer
        extends AbstractNodeVisitor<
                TransferResult<LiveVar, LiveVarStore>, TransferInput<LiveVar, LiveVarStore>>
        implements BackwardTransferFunction<LiveVar, LiveVarStore> {

    @Override
    public LiveVarStore initialNormalExitStore(
            UnderlyingAST underlyingAST, @Nullable List<ReturnNode> returnNodes) {
        return new LiveVarStore();
    }

    @Override
    public LiveVarStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
        return new LiveVarStore();
    }

    @Override
    public RegularTransferResult<LiveVar, LiveVarStore> visitNode(
            Node n, TransferInput<LiveVar, LiveVarStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore().copy());
    }

    @Override
    public RegularTransferResult<LiveVar, LiveVarStore> visitAssignment(
            AssignmentNode n, TransferInput<LiveVar, LiveVarStore> p) {
        RegularTransferResult<LiveVar, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVar, LiveVarStore>) super.visitAssignment(n, p);
        processLiveVarInAssignment(
                n.getTarget(), n.getExpression(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVar, LiveVarStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<LiveVar, LiveVarStore> p) {
        RegularTransferResult<LiveVar, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVar, LiveVarStore>)
                        super.visitStringConcatenateAssignment(n, p);
        processLiveVarInAssignment(
                n.getLeftOperand(), n.getRightOperand(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVar, LiveVarStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<LiveVar, LiveVarStore> p) {
        RegularTransferResult<LiveVar, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVar, LiveVarStore>) super.visitMethodInvocation(n, p);
        LiveVarStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<LiveVar, LiveVarStore> visitObjectCreation(
            ObjectCreationNode n, TransferInput<LiveVar, LiveVarStore> p) {
        RegularTransferResult<LiveVar, LiveVarStore> transferResult =
                (RegularTransferResult<LiveVar, LiveVarStore>) super.visitObjectCreation(n, p);
        LiveVarStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
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
        store.killLiveVar(new LiveVar(variable));
        store.addUseInExpression(expression);
    }
}
