package checkers.flow.constantpropagation;

import java.util.List;

import checkers.flow.analysis.AnalysisState;
import checkers.flow.cfg.node.EqualToNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;

import com.sun.source.tree.MethodTree;

public class ConstantPropagationConditionalTransfer extends
		ConstantPropagationTransfer {

	protected AnalysisState<Constant> analysisState;

	@Override
	public ConstantPropagationStore initialStore(MethodTree tree,
			List<LocalVariableNode> parameters) {
		return null;
	}

	@Override
	public ConstantPropagationStore visitNode(Node n, ConstantPropagationStore p) {
		return p;
	}
	
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
		Constant val = analysisState.getValue(a);
		if (b instanceof LocalVariableNode && val != null && val.isConstant()) {
			p.setInformation(((LocalVariableNode) b).getName(), val);
		}
	}

	@Override
	public void setAnalysisState(AnalysisState<Constant> as) {
		analysisState = as;
	}

}
