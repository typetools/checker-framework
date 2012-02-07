package checkers.flow.cfg.node;


/**
 * A visitor for a {@link Node} tree.
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
	R visitValueLiteral(ValueLiteralNode n, P p);
	R visitAssignment(AssignmentNode n, P p);
	R visitLocalVariable(LocalVariableNode n, P p);
	R visitVariableDeclaration(VariableDeclarationNode n, P p);
	R visitFieldAccess(FieldAccessNode n, P p);
	R visitIntegerLiteral(IntegerLiteralNode n, P p);
	R visitImplicitThisLiteral(ImplicitThisLiteralNode n, P p);
	R visitBooleanLiteral(BooleanLiteralNode n, P p);
	R visitConditionalOr(ConditionalOrNode n, P p);
	R visitEqualTo(EqualToNode n, P p);
	R visitReturn(ReturnNode n, P p);
	R visitNumericalAddition(NumericalAdditionNode n, P p);
        R visitStringLiteral(StringLiteralNode n, P p);
}
