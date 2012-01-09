package checkers.flow.constantpropagation;

import java.util.List;

import checkers.flow.analysis.TransferFunction;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.SinkNodeVisitor;
import checkers.flow.constantpropagation.Constant.Type;

import com.sun.source.tree.MethodTree;

public class ConstantPropagationTransfer extends
		SinkNodeVisitor<ConstantPropagationStore, ConstantPropagationStore>
		implements TransferFunction<ConstantPropagationStore> {

	@Override
	public ConstantPropagationStore initialStore(MethodTree tree,
			List<LocalVariableNode> parameters) {
		ConstantPropagationStore store = new ConstantPropagationStore();

		// we have no information about parameters
		for (LocalVariableNode p : parameters) {
			store.addInformation(p, new Constant(Type.TOP));
		}

		return store;
	}

	@Override
	public ConstantPropagationStore visitNode(Node n, ConstantPropagationStore p) {
		return p;
	}

	@Override
	public ConstantPropagationStore visitAssignment(AssignmentNode n,
			ConstantPropagationStore p) {
		Node target = n.getTarget();
		if (target instanceof LocalVariableNode) {
			LocalVariableNode t = (LocalVariableNode) target;
			p.setInformation(t, p.getInformation(n.getExpression()));
		}
		return p;
	}

	@Override
	public ConstantPropagationStore visitIntegerLiteral(IntegerLiteralNode n,
			ConstantPropagationStore p) {
		p.setInformation(n, new Constant(n.getValue()));
		return p;
	}

}
