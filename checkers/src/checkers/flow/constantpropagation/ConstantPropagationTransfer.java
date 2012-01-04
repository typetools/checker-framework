package checkers.flow.constantpropagation;

import checkers.flow.analysis.TransferFunction;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.SinkNodeVisitor;

public class ConstantPropagationTransfer extends
		SinkNodeVisitor<ConstantPropagationStore, ConstantPropagationStore>
		implements TransferFunction<Constant, ConstantPropagationStore> {

	@Override
	public ConstantPropagationStore visitNode(Node n, ConstantPropagationStore p) {
		return p.copy();
	}

	@Override
	public ConstantPropagationStore visitAssignment(AssignmentNode n,
			ConstantPropagationStore p) {
		ConstantPropagationStore r = p.copy();
		Node target = n.getTarget();
		if (target instanceof LocalVariableNode) {
			LocalVariableNode t = (LocalVariableNode) target;
			r.setInformation(t.getName(),
					p.getNodeInformation(n.getExpression()));
		}
		return r;
	}

	@Override
	public ConstantPropagationStore visitIntegerLiteral(IntegerLiteralNode n,
			ConstantPropagationStore p) {
		ConstantPropagationStore r = p.copy();
		r.setNodeInformation(n, new Constant(n.getValue()));
		return r;
	}

}
