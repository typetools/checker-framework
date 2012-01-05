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
		Constant leftVal = analysisState.getValue(left);
		Constant rightVal = analysisState.getValue(right);
		if (right instanceof LocalVariableNode && leftVal != null && leftVal.isConstant()) {
			p.setInformation(((LocalVariableNode) right).getName(), leftVal);
		}
		if (left instanceof LocalVariableNode && rightVal != null && rightVal.isConstant()) {
			p.setInformation(((LocalVariableNode) left).getName(), rightVal);
		}
		return p;
	}

	@Override
	public void setAnalysisState(AnalysisState<Constant> as) {
		analysisState = as;
	}

}
