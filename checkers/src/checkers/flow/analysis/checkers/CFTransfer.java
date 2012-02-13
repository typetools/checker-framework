package checkers.flow.analysis.checkers;

import java.util.List;

import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.node.AbstractNodeVisitor;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.StringLiteralNode;
import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * The default analysis transfer function for the Checker Framework propagates
 * information through assignments and uses the {@link AnnotatedTypeFactory} to
 * provide checker-specific logic how to combine types (e.g., what is the type
 * of a string concatenation, given the types of the two operands) and as an
 * abstraction function (e.g., determine the annotations on literals)..
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 */
public class CFTransfer
		extends
		AbstractNodeVisitor<TransferResult<CFValue, CFStore>, TransferInput<CFValue, CFStore>>
		implements TransferFunction<CFValue, CFStore> {

	/**
	 * The analysis class this store belongs to.
	 */
	protected CFAnalysis analysis;

	public CFTransfer(CFAnalysis analysis) {
		this.analysis = analysis;
	}

	/**
	 * The initial store maps method formal parameters to their currently most
	 * refined type.
	 */
	@Override
	public CFStore initialStore(MethodTree tree,
			List<LocalVariableNode> parameters) {
		CFStore info = new CFStore(analysis);

		for (LocalVariableNode p : parameters) {
			CFValue flowInsensitive = null; // TODO
			// assert flowInsensitive != null :
			// "Missing initial type information for method parameter";
			// info.mergeValue(p, flowInsensitive);
		}

		return info;
	}

	// TODO: We could use an intermediate classes such as ExpressionNode
	// to refactor visitors. Propagation is appropriate for all expressions.

	/**
	 * The default visitor returns the input information unchanged, or in the
	 * case of conditional input information, merged.
	 */
	@Override
	public TransferResult<CFValue, CFStore> visitNode(Node n,
			TransferInput<CFValue, CFStore> in) {
		// TODO: Perform type propagation separately with a thenStore and an
		// elseStore.
		CFStore info = in.getRegularStore();
		CFValue value = null;

		Tree tree = n.getTree();
		if (tree != null) {
			AnnotatedTypeMirror at = analysis.factory.getAnnotatedType(tree);
			value = new CFValue(analysis, at.getAnnotations());
		}

		return new RegularTransferResult<>(value, info);
	}

	/**
	 * Map local variable uses to their declarations and extract the most
	 * precise information available for the declaration.
	 */
	@Override
	public TransferResult<CFValue, CFStore> visitLocalVariable(
			LocalVariableNode n, TransferInput<CFValue, CFStore> in) {
		CFStore store = in.getRegularStore();
		CFValue value = store.getValue(n);
		// TODO: handle value == null (go to factory?)
		return new RegularTransferResult<>(value, store);
	}

	@Override
	public TransferResult<CFValue, CFStore> visitStringLiteral(
			StringLiteralNode n, TransferInput<CFValue, CFStore> p) {
		AnnotatedTypeMirror type = analysis.factory.getAnnotatedType(n
				.getTree());
		CFValue value = new CFValue(analysis, type.getAnnotations());
		return new RegularTransferResult<CFValue, CFStore>(value,
				p.getRegularStore());
	}

	/**
	 * Propagate information from the assignment's RHS to a variable on the LHS,
	 * if the RHS has more precise information available.
	 */
	@Override
	public TransferResult<CFValue, CFStore> visitAssignment(AssignmentNode n,
			TransferInput<CFValue, CFStore> in) {
		Node lhs = n.getTarget();
		Node rhs = n.getExpression();

		CFStore info = in.getRegularStore();
		CFValue rhsValue = in.getValueOfSubNode(rhs);

		if (rhsValue != null) {
			// assignment to a local variable
			if (lhs instanceof LocalVariableNode) {
				LocalVariableNode var = (LocalVariableNode) lhs;
				info.setValue(var, rhsValue);
			}
		}

		return new RegularTransferResult<>(rhsValue, info);
	}
}