package checkers.flow.cfg;

import checkers.flow.cfg.node.Node;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;

/**
 * This helper class builds the actual control flow graph for conditional
 * statements by visiting the abstract syntax tree. While
 * {@link CFGRegularHelper} is used to build the CFG for regular statements and
 * expressions, this class is concerned with the expressions inside a condition
 * that determines control flow (such as the condition of an if-expression or
 * the break condition of a while loop).
 * 
 * <p>
 * 
 * The difference to {@link CFGRegularHelper} is that two exit blocks are
 * maintained, one if the condition evaluated to true, and another for false.
 * 
 * <p>
 * 
 * The return type of this visitor is {@link Node}, and unlike
 * {@link CFGRegularHelper}, every visit method will return a non-null value.
 * 
 * @author Stefan Heule
 * 
 */
class CFGConditionalHelper implements TreeVisitor<Node, Void> {

	@Override
	public Node visitAnnotatedType(AnnotatedTypeTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitAnnotation(AnnotationTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitMethodInvocation(MethodInvocationTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitAssert(AssertTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitAssignment(AssignmentTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitBinary(BinaryTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitBlock(BlockTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitBreak(BreakTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitCase(CaseTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitCatch(CatchTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitClass(ClassTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitConditionalExpression(ConditionalExpressionTree node,
			Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitContinue(ContinueTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitDoWhileLoop(DoWhileLoopTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitErroneous(ErroneousTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitExpressionStatement(ExpressionStatementTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitForLoop(ForLoopTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitIdentifier(IdentifierTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitIf(IfTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitImport(ImportTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitArrayAccess(ArrayAccessTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitLabeledStatement(LabeledStatementTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitLiteral(LiteralTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitMethod(MethodTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitModifiers(ModifiersTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitNewArray(NewArrayTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitNewClass(NewClassTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitParenthesized(ParenthesizedTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitReturn(ReturnTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitMemberSelect(MemberSelectTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitEmptyStatement(EmptyStatementTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitSwitch(SwitchTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitSynchronized(SynchronizedTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitThrow(ThrowTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitCompilationUnit(CompilationUnitTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitTry(TryTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitParameterizedType(ParameterizedTypeTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitUnionType(UnionTypeTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitArrayType(ArrayTypeTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitTypeCast(TypeCastTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitPrimitiveType(PrimitiveTypeTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitTypeParameter(TypeParameterTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitInstanceOf(InstanceOfTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitUnary(UnaryTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitVariable(VariableTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitWhileLoop(WhileLoopTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitWildcard(WildcardTree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitOther(Tree node, Void p) {
		assert false; // TODO Auto-generated method stub
		return null;
	}

}
