package checkers.flow.constantpropagation;

import checkers.flow.cfg.node.EqualToNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;

public class ConstantPropagationConditionalTransfer extends
		ConstantPropagationTransfer {

	@Override
	public ConstantPropagationStore visitEqualTo(EqualToNode n,
			ConstantPropagationStore p) {
		Node left = n.getLeftOperand();
		Node right = n.getRightOperand();
		process(p, left, right);
		process(p, right, left);
		return p;
	}

	protected void process(ConstantPropagationStore p, Node a, Node b) {
		Constant val = p.getInformation(a);
		if (b instanceof LocalVariableNode && val.isConstant()) {
			p.setInformation(b, val);
		}
	}

}
