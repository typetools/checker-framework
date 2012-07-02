package checkers.flow.constantpropagation;

import java.util.List;

import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.node.AbstractNodeVisitor;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.EqualToNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;

public class ConstantPropagationTransfer
        extends
        AbstractNodeVisitor<TransferResult<Constant, ConstantPropagationStore>, TransferInput<Constant, ConstantPropagationStore>>
        implements TransferFunction<Constant, ConstantPropagationStore> {

    @Override
    public ConstantPropagationStore initialStore(UnderlyingAST underlyingAST,
            List<LocalVariableNode> parameters) {
        ConstantPropagationStore store = new ConstantPropagationStore();
        return store;
    }

    @Override
    public TransferResult<Constant, ConstantPropagationStore> visitNode(Node n,
            TransferInput<Constant, ConstantPropagationStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public TransferResult<Constant, ConstantPropagationStore> visitAssignment(
            AssignmentNode n,
            TransferInput<Constant, ConstantPropagationStore> pi) {
        ConstantPropagationStore p = pi.getRegularStore();
        Node target = n.getTarget();
        Constant info = null;
        if (target instanceof LocalVariableNode) {
            LocalVariableNode t = (LocalVariableNode) target;
            info = p.getInformation(n.getExpression());
            p.setInformation(t, info);
        }
        return new RegularTransferResult<>(info, p);
    }

    @Override
    public TransferResult<Constant, ConstantPropagationStore> visitIntegerLiteral(
            IntegerLiteralNode n,
            TransferInput<Constant, ConstantPropagationStore> pi) {
        ConstantPropagationStore p = pi.getRegularStore();
        Constant c = new Constant(n.getValue());
        p.setInformation(n, c);
        return new RegularTransferResult<>(c, p);
    }

    @Override
    public TransferResult<Constant, ConstantPropagationStore> visitEqualTo(
            EqualToNode n, TransferInput<Constant, ConstantPropagationStore> pi) {
        ConstantPropagationStore p = pi.getRegularStore();
        ConstantPropagationStore old = p.copy();
        Node left = n.getLeftOperand();
        Node right = n.getRightOperand();
        process(p, left, right);
        process(p, right, left);
        return new ConditionalTransferResult<>(null, p, old);
    }

    protected void process(ConstantPropagationStore p, Node a, Node b) {
        Constant val = p.getInformation(a);
        if (b instanceof LocalVariableNode && val.isConstant()) {
            p.setInformation(b, val);
        }
    }

}
