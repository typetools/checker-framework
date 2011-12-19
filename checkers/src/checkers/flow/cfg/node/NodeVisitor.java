package checkers.flow.cfg.node;


/**
 * A visitor for an {@link LocalVariableNode} tree.
 * 
 * @author Stefan Heule
 * 
 * @param <R>
 *            Return type of the visitor. Use {@link Void} if the visitor does
 *            not have a return.
 * @param <P>
 *            Parameter type of the visitor. Use {@link Void} if the visitor
 *            does not have a parameter.
 */
public interface NodeVisitor<R, P> {
	public R visitAssignment(AssignmentNode n, P p);
	public R visitIdentifier(LocalVariableNode n, P p);
	public R visitVariableDeclaration(VariableDeclarationNode n, P p);
	public R visitFieldAccess(FieldAccessNode n, P p);
	public R visitIntegerLiteral(IntegerLiteralNode n, P p);
	public R visitImplicitThisLiteral(ImplicitThisLiteralNode n, P p);
	public R visitBooleanLiteral(BooleanLiteralNode n, P p);
	public R visitConditionalOr(ConditionalOrNode n, P p);
}
