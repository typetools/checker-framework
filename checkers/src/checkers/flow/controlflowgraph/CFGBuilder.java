package checkers.flow.controlflowgraph;

import java.util.HashSet;
import java.util.Set;

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
import com.sun.source.util.TreeScanner;

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
	 * Determines the set of exceptions that can possibly be thrown by a
	 * {@link Tree} t.
	 * 
	 * @return A set of exceptions that might be thrown by a tree t.
	 */
	protected Set<Class<? extends Throwable>> possibleExceptions(Tree t) {
		// TODO are these all sources of exceptions?
		final Set<Class<? extends Throwable>> exceptions = new HashSet<Class<? extends Throwable>>();
		t.accept(new TreeScanner<Void, Void>() {
			@Override
			public Void visitBinary(BinaryTree node, Void p) {
				if (node.getKind().equals(Tree.Kind.DIVIDE)
						|| node.getKind().equals(Tree.Kind.REMAINDER)) {
					exceptions.add(ArithmeticException.class); // division by 0
				}
				return super.visitBinary(node, p);
			}

			@Override
			public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
				// a method can throw arbitrary unchecked exceptions ..
				exceptions.add(RuntimeException.class);
				exceptions.add(Error.class);
				// .. and checked exceptions as declared
				// TODO checked exceptions
				return super.visitMethodInvocation(node, p);
			}

			@Override
			public Void visitMemberSelect(MemberSelectTree node, Void p) {
				exceptions.add(NullPointerException.class);
				return super.visitMemberSelect(node, p);
			}
		}, null);
		return exceptions;
	}

	/**
	 * This helper class builds the actual control flow graph by visiting the
	 * abstract syntax tree. A class separate from {@link CFGBuilder} is used to
	 * hide implementation details and keep the interface of {@link CFGBuilder}
	 * clean.
	 * 
	 * @author Stefan Heule
	 * 
	 */
	protected class CFGHelper implements TreeVisitor<Void, Void> {

		/** The basic block that is currently being filled with contents. */
		protected BasicBlockImplementation currentBlock;

		/**
		 * Build the control flow graph for a {@link BlockTree} that represents
		 * a methods body.
		 * 
		 * @param t
		 *            Method body.
		 * @return The entry node of the resulting control flow graph.
		 */
		public BasicBlock build(BlockTree t) {
			BasicBlockImplementation startBlock = new BasicBlockImplementation();
			currentBlock = startBlock;
			t.accept(this, null);
			return startBlock;
		}

		@Override
		public Void visitAnnotatedType(AnnotatedTypeTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitAnnotation(AnnotationTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitAssert(AssertTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitAssignment(AssignmentTree node, Void p) {
			// TODO exceptions
			currentBlock.addStatement(node);
			return null;
		}

		@Override
		public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
			// TODO exceptions
			currentBlock.addStatement(node);
			return null;
		}

		@Override
		public Void visitBinary(BinaryTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitBlock(BlockTree node, Void p) {
			for (StatementTree n : node.getStatements()) {
				n.accept(this, null);
			}
			return null;
		}

		@Override
		public Void visitBreak(BreakTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitCase(CaseTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitCatch(CatchTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitClass(ClassTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitConditionalExpression(ConditionalExpressionTree node,
				Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitContinue(ContinueTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitErroneous(ErroneousTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitExpressionStatement(ExpressionStatementTree node,
				Void p) {
			// TODO exceptions
			currentBlock.addStatement(node);
			return null;
		}

		@Override
		public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitForLoop(ForLoopTree node, Void p) {
			BasicBlockImplementation initBlock = new BasicBlockImplementation();
			ConditionalBasicBlockImplementation conditionBlock = new ConditionalBasicBlockImplementation();
			BasicBlockImplementation afterBlock = new BasicBlockImplementation();
			BasicBlockImplementation loopBodyBlock = new BasicBlockImplementation();

			initBlock.addStatements(node.getInitializer());

			conditionBlock.setCondition(node.getCondition());

			// visit the initialization statements
			for (StatementTree t : node.getInitializer()) {
				t.accept(this, null);
			}

			currentBlock.addSuccessor(conditionBlock);
			conditionBlock.setThenSuccessor(loopBodyBlock);
			conditionBlock.setElseSuccessor(afterBlock);

			currentBlock = loopBodyBlock;
			node.getStatement().accept(this, null);
			for (StatementTree t : node.getUpdate()) {
				t.accept(this, null);
			}
			currentBlock.addSuccessor(conditionBlock);

			currentBlock = afterBlock;
			return null;
		}

		@Override
		public Void visitIdentifier(IdentifierTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitIf(IfTree node, Void p) {

			// TODO exceptions
			BasicBlockImplementation afterIfBlock = new BasicBlockImplementation();

			// basic block for the condition
			ConditionalBasicBlockImplementation conditionalBlock = new ConditionalBasicBlockImplementation();
			conditionalBlock.setCondition(node.getCondition());
			currentBlock.addSuccessor(conditionalBlock);

			// then branch
			currentBlock = new BasicBlockImplementation();
			conditionalBlock.setThenSuccessor(currentBlock);
			StatementTree thenStatement = node.getThenStatement();
			thenStatement.accept(this, null);
			currentBlock.addSuccessor(afterIfBlock);

			// else branch
			StatementTree elseStatement = node.getElseStatement();
			if (elseStatement != null) {
				currentBlock = new BasicBlockImplementation();
				conditionalBlock.setElseSuccessor(currentBlock);
				elseStatement.accept(this, null);
				currentBlock.addSuccessor(afterIfBlock);
			} else {
				conditionalBlock.setElseSuccessor(afterIfBlock);
			}

			currentBlock = afterIfBlock;

			return null;
		}

		@Override
		public Void visitImport(ImportTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitArrayAccess(ArrayAccessTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitLabeledStatement(LabeledStatementTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitLiteral(LiteralTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitMethod(MethodTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitModifiers(ModifiersTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitNewArray(NewArrayTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitNewClass(NewClassTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitParenthesized(ParenthesizedTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitReturn(ReturnTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitMemberSelect(MemberSelectTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitEmptyStatement(EmptyStatementTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitSwitch(SwitchTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitSynchronized(SynchronizedTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitThrow(ThrowTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitTry(TryTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitParameterizedType(ParameterizedTypeTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitUnionType(UnionTypeTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitArrayType(ArrayTypeTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitTypeCast(TypeCastTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitPrimitiveType(PrimitiveTypeTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitTypeParameter(TypeParameterTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitInstanceOf(InstanceOfTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitUnary(UnaryTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitVariable(VariableTree node, Void p) {
			// TODO exceptions
			currentBlock.addStatement(node);
			return null;
		}

		@Override
		public Void visitWhileLoop(WhileLoopTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitWildcard(WildcardTree node, Void p) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Void visitOther(Tree node, Void p) {
			// TODO better handling
			throw new RuntimeException(
					"Unknown AST element encountered in AST to CFG translation.");
		}

	}
}
