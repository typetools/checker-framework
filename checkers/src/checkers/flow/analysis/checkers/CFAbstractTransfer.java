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
public abstract class CFAbstractTransfer<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>, T extends CFAbstractTransfer<V, S, T>>
		extends AbstractNodeVisitor<TransferResult<V, S>, TransferInput<V, S>>
		implements TransferFunction<V, S> {

	/**
	 * The analysis class this store belongs to.
	 */
	protected CFAbstractAnalysis<V, S, T> analysis;

	public CFAbstractTransfer(CFAbstractAnalysis<V, S, T> analysis) {
		this.analysis = analysis;
	}

	/**
	 * The initial store maps method formal parameters to their currently most
	 * refined type.
	 */
	@Override
	public S initialStore(MethodTree tree, List<LocalVariableNode> parameters) {
		S info = analysis.createEmptyStore();

		for (LocalVariableNode p : parameters) {
			V flowInsensitive = null; // TODO
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
	public TransferResult<V, S> visitNode(Node n, TransferInput<V, S> in) {
		// TODO: Perform type propagation separately with a thenStore and an
		// elseStore.
		S info = in.getRegularStore();
		V value = null;

		Tree tree = n.getTree();
		if (tree != null) {
			AnnotatedTypeMirror at = analysis.factory.getAnnotatedType(tree);
			value = analysis.createAbstractValue(at.getAnnotations());
		}

		return new RegularTransferResult<>(value, info);
	}

	/**
	 * Map local variable uses to their declarations and extract the most
	 * precise information available for the declaration.
	 */
	@Override
	public TransferResult<V, S> visitLocalVariable(LocalVariableNode n,
			TransferInput<V, S> in) {
		S store = in.getRegularStore();
		V value = store.getValue(n);
		// TODO: handle value == null (go to factory?)
		return new RegularTransferResult<>(value, store);
	}

	@Override
	public TransferResult<V, S> visitStringLiteral(StringLiteralNode n,
			TransferInput<V, S> p) {
		AnnotatedTypeMirror type = analysis.factory.getAnnotatedType(n
				.getTree());
		V value = analysis.createAbstractValue(type.getAnnotations());
		return new RegularTransferResult<>(value, p.getRegularStore());
	}

	/**
	 * Propagate information from the assignment's RHS to a variable on the LHS,
	 * if the RHS has more precise information available.
	 */
	@Override
	public TransferResult<V, S> visitAssignment(AssignmentNode n,
			TransferInput<V, S> in) {
		Node lhs = n.getTarget();
		Node rhs = n.getExpression();

		S info = in.getRegularStore();
		V rhsValue = in.getValueOfSubNode(rhs);

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