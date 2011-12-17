package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.Set;

import checkers.flow.controlflowgraph.SpecialBasicBlockImplementation.SpecialBasicBlockTypes;
import checkers.flow.controlflowgraph.node.AssignmentNode;
import checkers.flow.controlflowgraph.node.BooleanLiteralNode;
import checkers.flow.controlflowgraph.node.ConditionalOrNode;
import checkers.flow.controlflowgraph.node.FieldAccessNode;
import checkers.flow.controlflowgraph.node.IdentifierNode;
import checkers.flow.controlflowgraph.node.ImplicitThisLiteralNode;
import checkers.flow.controlflowgraph.node.IntegerLiteralNode;
import checkers.flow.controlflowgraph.node.Node;
import checkers.flow.controlflowgraph.node.NodeUtils;
import checkers.flow.util.ASTUtils;

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
import com.sun.source.tree.ExpressionTree;
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
import com.sun.source.tree.StatementTree;
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
 * Builds the control flow graph of a Java method (represented by its abstract
 * syntax tree, {@link MethodTree}).
 * 
 * @author Stefan Heule
 * 
 */
public class CFGBuilder {

	/**
	 * Build the control flow graph of a method.
	 */
	public BasicBlock build(MethodTree method) {
		return new CFGHelper().build(method.getBody());
	}

	/**
	 * This helper class builds the actual control flow graph by visiting the
	 * abstract syntax tree. A class separate from {@link CFGBuilder} is used to
	 * hide implementation details and keep the interface of {@link CFGBuilder}
	 * clean.
	 * 
	 * <p>
	 * 
	 * The return type of this visitor is {@link Node}. For expressions, the
	 * corresponding node is returned to allow linking between different nodes.
	 * For instance, in visitAssignment, we can run the visitor on the left and
	 * right hand side and store the result as target and expression in the
	 * newly created {@link AssignmentNode}.
	 * 
	 * However, for statements there is usually no single {@link Node} that is
	 * created, and thus no node is returned (rather, null is returned).
	 * 
	 * @author Stefan Heule
	 * 
	 */
	protected class CFGHelper implements TreeVisitor<Node, Void> {

		/**
		 * The basic block that is currently being filled with contents.
		 * 
		 * <p>
		 * 
		 * <strong>Important:</strong> Contents should be added through
		 * {@link addToCurrentBlock} instead of directly accessing
		 * {@link currentBlock}. Furthermore, the following invariant holds
		 * during CFG construction:
		 * 
		 * <pre>
		 * currentBlock == null  <==>  predecessors != null
		 * </pre>
		 * 
		 * <code>currentBlock</code> can be <code>null</code> to indicate that
		 * there is currently not a single current block, but rather a set of
		 * predecessors. This happens, for instance after an if-block where the
		 * then and else branch are the predecessors. If
		 * <code>currentBlock</code> is <code>null</code>, then adding a block
		 * will make all blocks in <code>predecessors</code> a predecessor of
		 * the newly created block.
		 */
		protected BasicBlockImplementation currentBlock;

		/**
		 * Predecessors, details see <code>currentBlock</code>.
		 */
		protected Set<BasicBlockImplementation> predecessors;

		/**
		 * The exceptional exit basic block (which might or might not be used).
		 */
		protected SpecialBasicBlockImplementation exceptionalExitBlock;

		/**
		 * Build the control flow graph for a {@link BlockTree} that represents
		 * a methods body.
		 * 
		 * @param t
		 *            Method body.
		 * @return The entry node of the resulting control flow graph.
		 */
		public BasicBlock build(BlockTree t) {
			// create start block
			BasicBlockImplementation startBlock = new SpecialBasicBlockImplementation(
					SpecialBasicBlockTypes.ENTRY);
			currentBlock = startBlock;

			// create exceptional end block
			exceptionalExitBlock = new SpecialBasicBlockImplementation(
					SpecialBasicBlockTypes.EXCEPTIONAL_EXIT);

			// traverse AST
			t.accept(this, null);

			// finish CFG
			SpecialBasicBlockImplementation exit = new SpecialBasicBlockImplementation(
					SpecialBasicBlockTypes.EXIT);
			if (currentBlock == null) {
				if (predecessors.size() > 0) {
					for (BasicBlockImplementation p : predecessors) {
						p.addSuccessor(exit);
					}
				}
			} else {
				currentBlock.addSuccessor(exit);
			}
			return startBlock;
		}

		/**
		 * Add a node to the current basic block, automatically deciding whether
		 * it has to be a {@link ConditionalBasicBlock}.
		 * 
		 * @param node
		 *            The node to add.
		 * @return The same node (for convenience).
		 */
		protected Node addToCurrentBlock(Node node) {
			if (NodeUtils.isBooleanTypeNode(node)) {
				ConditionalBasicBlockImplementation cb = new ConditionalBasicBlockImplementation();
				cb.addStatement(node);
				currentBlock.addSuccessor(cb);
				currentBlock = cb;
			} else {
				if (currentBlock instanceof ConditionalBasicBlockImplementation) {
					// a conditional basic block only contains a single boolean
					// expression, therefore we create a new basic block here
					BasicBlockImplementation bb = new BasicBlockImplementation();
					bb.addStatement(node);
					currentBlock.addSuccessor(bb);
					currentBlock = bb;
				} else {
					if (currentBlock instanceof SpecialBasicBlockImplementation) {
						BasicBlockImplementation old = currentBlock;
						currentBlock = new BasicBlockImplementation();
						old.addSuccessor(currentBlock);
					}
					currentBlock.addStatement(node);
				}
			}
			return node;
		}

		/**
		 * @return The current basic block as conditional basic block (only
		 *         applicable if {@link currentBlock} is a conditional basic
		 *         block.
		 */
		protected ConditionalBasicBlockImplementation getCurrentConditionalBasicBlock() {
			assert currentBlock instanceof ConditionalBasicBlockImplementation;
			return (ConditionalBasicBlockImplementation) currentBlock;
		}

		@Override
		public Node visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitAnnotation(AnnotationTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitMethodInvocation(MethodInvocationTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitAssert(AssertTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitAssignment(AssignmentTree tree, Void p) {

			// see JLS 15.26.1

			Node expression;
			ExpressionTree variable = tree.getVariable();

			// case 1: field access
			if (ASTUtils.isFieldAccess(variable)) {
				// visit receiver
				Node receiver = getReceiver(variable);

				// visit expression
				expression = tree.getExpression().accept(this, p);

				// visit field access (throws null-pointer exception)
				// TODO: exception
				String field = ASTUtils.getFieldName(variable);
				FieldAccessNode target = new FieldAccessNode(variable,
						receiver, field);
				addToCurrentBlock(target);

				// add assignment node
				AssignmentNode assignmentNode = new AssignmentNode(tree,
						target, expression);
				addToCurrentBlock(assignmentNode);
			}

			// TODO: case 2: array access

			// case 3: other cases
			else {
				Node target = variable.accept(this, p);
				expression = tree.getExpression().accept(this, p);
				AssignmentNode assignmentNode = new AssignmentNode(tree,
						target, expression);
				addToCurrentBlock(assignmentNode);
			}
			return expression;
		}

		/**
		 * Note 1: Requires <code>tree</code> to be a field access tree.
		 * <p>
		 * Node 2: Visits the receiver and adds all necessary blocks to the CFG.
		 * 
		 * @return The receiver of the field access.
		 */
		private Node getReceiver(Tree tree) {
			assert ASTUtils.isFieldAccess(tree);
			if (tree.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
				MemberSelectTree mtree = (MemberSelectTree) tree;
				return mtree.getExpression().accept(this, null);
			} else {
				Node node = new ImplicitThisLiteralNode();
				addToCurrentBlock(node);
				return node;
			}
		}

		@Override
		public Node visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitBinary(BinaryTree tree, Void p) {
			// TODO: remaining binary node types
			Node r = null;
			switch (tree.getKind()) {
			case CONDITIONAL_OR:
				Node left = tree.getLeftOperand().accept(this, p);
				ConditionalBasicBlockImplementation lhsBB = getCurrentConditionalBasicBlock();
				Node right = tree.getRightOperand().accept(this, p);
				lhsBB.setElseSuccessor(currentBlock);
				r = new ConditionalOrNode(tree, left, right);
				addToCurrentBlock(r);
				lhsBB.setThenSuccessor(currentBlock);
				return r;
			}
			assert r != null : "unexpected binary tree";
			return addToCurrentBlock(r);
		}

		@Override
		public Node visitBlock(BlockTree tree, Void p) {
			for (StatementTree n : tree.getStatements()) {
				n.accept(this, null);
			}
			return null;
		}

		@Override
		public Node visitBreak(BreakTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCase(CaseTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCatch(CatchTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitClass(ClassTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitConditionalExpression(ConditionalExpressionTree tree,
				Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitContinue(ContinueTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitErroneous(ErroneousTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitExpressionStatement(ExpressionStatementTree tree,
				Void p) {
			return tree.getExpression().accept(this, p);
		}

		@Override
		public Node visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitForLoop(ForLoopTree tree, Void p) {
			/*
			 * BasicBlockImplementation initBlock = new
			 * BasicBlockImplementation(); ConditionalBasicBlockImplementation
			 * conditionBlock = new ConditionalBasicBlockImplementation();
			 * BasicBlockImplementation afterBlock = new
			 * BasicBlockImplementation(); BasicBlockImplementation
			 * loopBodyBlock = new BasicBlockImplementation();
			 * 
			 * initBlock.addStatements(tree.getInitializer());
			 * 
			 * conditionBlock.setCondition(tree.getCondition());
			 * 
			 * // visit the initialization statements for (StatementTree t :
			 * tree.getInitializer()) { t.accept(this, null); }
			 * 
			 * currentBlock.addSuccessor(conditionBlock);
			 * conditionBlock.setThenSuccessor(loopBodyBlock);
			 * conditionBlock.setElseSuccessor(afterBlock);
			 * 
			 * currentBlock = loopBodyBlock; tree.getStatement().accept(this,
			 * null); for (StatementTree t : tree.getUpdate()) { t.accept(this,
			 * null); } currentBlock.addSuccessor(conditionBlock);
			 * 
			 * currentBlock = afterBlock;
			 */
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitIdentifier(IdentifierTree tree, Void p) {
			return addToCurrentBlock(new IdentifierNode(tree));
		}

		@Override
		public Node visitIf(IfTree tree, Void p) {

			// TODO exceptions
			Set<BasicBlockImplementation> newPredecessors = new HashSet<>();

			// basic block for the condition
			tree.getCondition().accept(this, null);
			assert currentBlock instanceof ConditionalBasicBlockImplementation;
			ConditionalBasicBlockImplementation conditionalBlock = (ConditionalBasicBlockImplementation) currentBlock;

			// then branch
			currentBlock = new BasicBlockImplementation();
			conditionalBlock.setThenSuccessor(currentBlock);
			StatementTree thenStatement = tree.getThenStatement();
			thenStatement.accept(this, null);
			newPredecessors.add(currentBlock);

			// else branch
			StatementTree elseStatement = tree.getElseStatement();
			if (elseStatement != null) {
				currentBlock = new BasicBlockImplementation();
				conditionalBlock.setElseSuccessor(currentBlock);
				elseStatement.accept(this, null);
				newPredecessors.add(currentBlock);
			} else {
				newPredecessors.add(conditionalBlock);
			}

			currentBlock = null;
			predecessors = newPredecessors;
			return null;
		}

		@Override
		public Node visitImport(ImportTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitArrayAccess(ArrayAccessTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitLabeledStatement(LabeledStatementTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitLiteral(LiteralTree tree, Void p) {
			// TODO: remaining literals
			Node r = null;
			switch (tree.getKind()) {
			case INT_LITERAL:
				r = new IntegerLiteralNode(tree);
				break;
			case BOOLEAN_LITERAL:
				r = new BooleanLiteralNode(tree);
				break;
			}
			assert r != null : "unexpected literal tree";
			return addToCurrentBlock(r);
		}

		@Override
		public Node visitMethod(MethodTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitModifiers(ModifiersTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitNewArray(NewArrayTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitNewClass(NewClassTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitParenthesized(ParenthesizedTree tree, Void p) {
			return tree.getExpression().accept(this, p);
		}

		@Override
		public Node visitReturn(ReturnTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitMemberSelect(MemberSelectTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		protected Node translateFieldAccess(Tree tree) {
			assert ASTUtils.isFieldAccess(tree);
			// visit receiver
			Node receiver = getReceiver(tree);

			// visit field access (throws null-pointer exception)
			// TODO: exception
			String field = ASTUtils.getFieldName(tree);
			FieldAccessNode access = new FieldAccessNode(tree, receiver,
					field);
			addToCurrentBlock(access);
			return access;
		}

		@Override
		public Node visitEmptyStatement(EmptyStatementTree tree, Void p) {
			return null;
		}

		@Override
		public Node visitSwitch(SwitchTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitSynchronized(SynchronizedTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitThrow(ThrowTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCompilationUnit(CompilationUnitTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTry(TryTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitParameterizedType(ParameterizedTypeTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitUnionType(UnionTypeTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitArrayType(ArrayTypeTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTypeCast(TypeCastTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitPrimitiveType(PrimitiveTypeTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTypeParameter(TypeParameterTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitInstanceOf(InstanceOfTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitUnary(UnaryTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitVariable(VariableTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitWhileLoop(WhileLoopTree tree, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitWildcard(WildcardTree tree, Void p) {
			assert false : "WildcardTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitOther(Tree tree, Void p) {
			assert false : "Unknown AST element encountered in AST to CFG translation.";
			return null;
		}

	}
}
