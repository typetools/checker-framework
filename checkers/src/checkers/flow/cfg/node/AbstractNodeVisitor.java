package checkers.flow.cfg.node;

/**
 * A default implementation of the node visitor interface. The class introduces
 * several 'summary' methods, that can be overridden to change the behavior of
 * several related visit methods at once. An example is the
 * {@code visitValueLiteral} method, that is called for every
 * {@link ValueLiteralNode}.
 * 
 * <p>
 * 
 * This is useful to implement a visitor that performs the same operation (e.g.,
 * nothing) for most {@link Node}s and only has special behavior for a few.
 * 
 * @author Stefan Heule
 * 
 * @param <R>
 *            Return type of the visitor.
 * @param <P>
 *            Parameter type of the visitor.
 */
public abstract class AbstractNodeVisitor<R, P> implements NodeVisitor<R, P> {

	abstract public R visitNode(Node n, P p);

	public R visitValueLiteral(ValueLiteralNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitAssignment(AssignmentNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitLocalVariable(LocalVariableNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitVariableDeclaration(VariableDeclarationNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitFieldAccess(FieldAccessNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitIntegerLiteral(IntegerLiteralNode n, P p) {
		return visitValueLiteral(n, p);
	}

	@Override
	public R visitImplicitThisLiteral(ImplicitThisLiteralNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitBooleanLiteral(BooleanLiteralNode n, P p) {
		return visitValueLiteral(n, p);
	}

	@Override
	public R visitConditionalOr(ConditionalOrNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitEqualTo(EqualToNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitReturn(ReturnNode n, P p) {
		return visitNode(n, p);
	};

	@Override
	public R visitNumericalAddition(NumericalAdditionNode n, P p) {
		return visitNode(n, p);
	}

	@Override
	public R visitStringLiteral(StringLiteralNode n, P p) {
		return visitValueLiteral(n, p);
	}

}
