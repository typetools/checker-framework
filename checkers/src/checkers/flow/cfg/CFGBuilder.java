package checkers.flow.cfg;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import checkers.flow.cfg.block.Block.BlockType;
import checkers.flow.cfg.block.BlockImpl;
import checkers.flow.cfg.block.ConditionalBlockImpl;
import checkers.flow.cfg.block.RegularBlockImpl;
import checkers.flow.cfg.block.SingleSuccessorBlockImpl;
import checkers.flow.cfg.block.SpecialBlock;
import checkers.flow.cfg.block.SpecialBlock.SpecialBlockType;
import checkers.flow.cfg.block.SpecialBlockImpl;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.BooleanLiteralNode;
import checkers.flow.cfg.node.ConditionalOrNode;
import checkers.flow.cfg.node.EqualToNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.VariableDeclarationNode;
import checkers.flow.util.ASTUtils;
import checkers.flow.util.NodeUtils;

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
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
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
	public static ControlFlowGraph build(MethodTree method) {
		return new CFGHelper().build(method);
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
	 * 
	 * However, for statements there is usually no single {@link Node} that is
	 * created, and thus no node is returned (rather, null is returned).
	 * 
	 * @author Stefan Heule
	 * 
	 */
	protected static class CFGHelper implements TreeVisitor<Node, Void> {

		/**
		 * The {@link CFGHelper} visits the AST to build the control flow graph.
		 * To correctly link the basic blocks, three fields are used:
		 * <code>currentBlock</code>, <code>truePredecessors</code> and
		 * <code>falsePredecessors</code>. If there is one currently active
		 * block and this block is a regular block (and not a conditional basic
		 * block), then <code>currentBlock</code> is used to store this block.
		 * Further nodes can still be added to this block. Otherwise, that is if
		 * there are multiple blocks to which the next block should be added as
		 * successor (e.g. after an if statement), or if the last block is a
		 * conditional basic block, then <code>truePredecessors</code> and
		 * <code>falsePredecessors</code> are used to store these blocks (both
		 * fields store the predecessor, for the difference see explanation of
		 * <code>conditionalMode</code>).
		 * 
		 * The following invariant is maintained:
		 * 
		 * <pre>
		 *   currentBlock == null   <==>  (truePredecessors != null && falsePredecessors != null)
		 * </pre>
		 * 
		 * These three fields should only be managed by the methods below that
		 * are titled 'Manage Predecessors' and 'Basic Block Linking'.
		 */
		protected RegularBlockImpl currentBlock;

		/** See description of <code>currentBlock</code>. */
		protected PredecessorBlockHolder truePredecessors;

		/** See description of <code>currentBlock</code>. */
		protected PredecessorBlockHolder falsePredecessors;

		/**
		 * The translation starts in regular mode, that is
		 * <code>conditionalMode</code> is false. In this case, no conditional
		 * basic blocks are generated.
		 * 
		 * To correctly model control flow when the evaluation of an expression
		 * determines control flow (e.g. for if-conditions, while loops, or
		 * short-circuiting conditional expressions),
		 * <code>conditionalMode</code> can be set to true. Then, the fields
		 * <code>truePredecessors</code> and <code>falsePredecessors</code> are
		 * used to store the predecessor blocks when the expression just visited
		 * evaluates to true or false, respectively.
		 */
		protected boolean conditionalMode;

		/**
		 * The exceptional exit basic block (which might or might not be used).
		 */
		protected SpecialBlock exceptionalExitBlock;

		/** Map from AST {@link Tree}s to {@link Node}s. */
		// TODO: fill this map with contents.
		protected IdentityHashMap<Tree, Node> treeLookupMap;

		/* --------------------------------------------------------- */
		/* Translation (AST to CFG) */
		/* --------------------------------------------------------- */

		/**
		 * Build the control flow graph for a {@link BlockTree} that represents
		 * a methods body.
		 * 
		 * @param t
		 *            Method body.
		 * @return The resulting control flow graph.
		 */
		public ControlFlowGraph build(MethodTree t) {

			// start in regular mode
			conditionalMode = false;

			// start with empty map
			treeLookupMap = new IdentityHashMap<>();

			// create start block
			SpecialBlockImpl startBlock = new SpecialBlockImpl(
					SpecialBlockType.ENTRY);
			setSingleAnyPredecessor(startBlock);

			// create exceptional end block
			exceptionalExitBlock = new SpecialBlockImpl(
					SpecialBlockType.EXCEPTIONAL_EXIT);

			// traverse AST of the method body
			t.getBody().accept(this, null);

			// finish CFG
			SpecialBlockImpl exit = new SpecialBlockImpl(SpecialBlockType.EXIT);
			extendWithBasicBlock(exit);

			return new ControlFlowGraph(startBlock, t, treeLookupMap);
		}

		/* --------------------------------------------------------- */
		/* Manage Predecessors */
		/* --------------------------------------------------------- */

		/**
		 * Used to keep track of the predecessors (to allow setting their
		 * successor appropriately when a new block is added).
		 * 
		 * The reason a simple list is not sufficient, is the following: For
		 * {@link ConditionalBasicBlock}s, there are two possibilities to set
		 * the successor, and a simple list would not give the information which
		 * one to use.
		 */
		protected abstract static class PredecessorBlockHolder {
			abstract public void setSuccessorAs(BlockImpl b);

			/**
			 * List of the string representation of all components in this
			 * predecessor list (used for toString).
			 */
			abstract protected List<String> componentList();

			@Override
			public String toString() {
				return componentList().toString();
			}
		}

		/**
		 * Retrieve the contents of <code>falsePredecessors</code> and set its
		 * value to null.
		 */
		protected PredecessorBlockHolder getAndResetFalsePredecessors() {
			PredecessorBlockHolder old = falsePredecessors;
			falsePredecessors = null;
			return old;
		}

		/**
		 * Retrieve the contents of <code>truePredecessors</code> and set its
		 * value to null.
		 */
		protected PredecessorBlockHolder getAndResetTruePredecessors() {
			PredecessorBlockHolder old = truePredecessors;
			truePredecessors = null;
			return old;
		}

		/**
		 * Set a single basic block as true predecessor.
		 * 
		 * @param bb
		 *            The basic block to set.
		 */
		protected void setSingleTruePredecessor(
				final SingleSuccessorBlockImpl bb) {
			truePredecessors = singletonPredecessor(bb);
		}

		/**
		 * Set a single basic block as false predecessor.
		 * 
		 * @param bb
		 *            The basic block to set.
		 */
		protected void setSingleFalsePredecessor(
				final SingleSuccessorBlockImpl bb) {
			falsePredecessors = singletonPredecessor(bb);
		}

		/**
		 * Set a single conditional basic block as true predecessor, using the
		 * then-successor.
		 * 
		 * @param cb
		 *            The basic block to set.
		 */
		protected void setThenAsTruePredecessor(final ConditionalBlockImpl cb) {
			assert truePredecessors == null;
			truePredecessors = new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BlockImpl b) {
					cb.setThenSuccessor(b);
				}

				@Override
				protected List<String> componentList() {
					return Collections.singletonList(cb.toString() + "<then>");
				}
			};
		}

		/**
		 * Set a single conditional basic block as false predecessor, using the
		 * else-successor.
		 * 
		 * @param cb
		 *            The basic block to set.
		 */
		protected void setElseAsFalsePredecessor(final ConditionalBlockImpl cb) {
			assert falsePredecessors == null;
			falsePredecessors = new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BlockImpl b) {
					cb.setElseSuccessor(b);
				}

				@Override
				protected List<String> componentList() {
					return Collections.singletonList(cb.toString() + "<else>");
				}
			};
		}

		/**
		 * Set a single basic block as the predecessor (not distinguishing
		 * true/false).
		 * 
		 * @param more
		 *            The basic block to set.
		 */
		protected void setSingleAnyPredecessor(SingleSuccessorBlockImpl bb) {
			assert bb != null;
			currentBlock = null;
			truePredecessors = singletonPredecessor(bb);
			falsePredecessors = singletonPredecessor(bb);
		}

		/**
		 * Set a set of predecessors as the predecessors (not distinguishing
		 * true/false).
		 * 
		 * @param more
		 *            The predecessors to set.
		 */
		protected void setAnyPredecessor(PredecessorBlockHolder h) {
			assert h != null;
			currentBlock = null;
			truePredecessors = h;
			falsePredecessors = h;
		}

		/**
		 * Reset all predecessors to null.
		 */
		protected void clearAnyPredecessor() {
			truePredecessors = null;
			falsePredecessors = null;
		}

		/**
		 * Add a set of predecessors (identified by <code>more</code>) to the
		 * predecessors (not distinguishing true/false).
		 * 
		 * @param more
		 *            The predecessors to add.
		 */
		protected void addAnyPredecessor(final PredecessorBlockHolder more) {
			truePredecessors = combinePredecessors(truePredecessors, more);
			falsePredecessors = combinePredecessors(falsePredecessors, more);
		}

		/**
		 * @return A {@link PredecessorBlockHolder} that stores the basic block
		 *         <code>bb</code>.
		 */
		protected static PredecessorBlockHolder singletonPredecessor(
				final SingleSuccessorBlockImpl bb) {
			return new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BlockImpl b) {
					bb.setSuccessor(b);
				}

				@Override
				protected List<String> componentList() {
					return Collections.singletonList(bb.toString());
				}
			};
		}

		/**
		 * Combine two {@link PredecessorBlockHolder}s to a single one
		 * containing all basic blocks.
		 */
		protected static PredecessorBlockHolder combinePredecessors(
				final PredecessorBlockHolder a, final PredecessorBlockHolder b) {
			return new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BlockImpl c) {
					if (a != null) {
						a.setSuccessorAs(c);
					}
					if (b != null) {
						b.setSuccessorAs(c);
					}
				}

				@Override
				protected List<String> componentList() {
					List<String> l = new LinkedList<>();
					if (a != null) {
						l.addAll(a.componentList());
					}
					if (b != null) {
						l.addAll(b.componentList());
					}
					return l;
				}
			};
		}

		/**
		 * Add the current block to a predecessor list (or the predecessors if
		 * currentBlock is null).
		 * 
		 * Note: newPredecessors can also be null.
		 */
		protected PredecessorBlockHolder addCurrentPredecessorToPredecessor(
				final PredecessorBlockHolder newPredecessors) {
			if (currentBlock != null) {
				return combinePredecessors(singletonPredecessor(currentBlock),
						newPredecessors);
			} else {
				return combinePredecessors(falsePredecessors,
						combinePredecessors(truePredecessors, newPredecessors));
			}
		}

		/* --------------------------------------------------------- */
		/* Basic Block Linking */
		/* --------------------------------------------------------- */

		/**
		 * Extend the CFG with a node.
		 * 
		 * @param node
		 *            The node to add.
		 * @return The same node (for convenience).
		 */
		protected Node extendWithNode(Node node) {
			if (conditionalMode) {
				ConditionalBlockImpl cb = new ConditionalBlockImpl();
				cb.setCondition(node);
				extendWithBasicBlock(cb);
				return node;
			} else {
				if (currentBlock == null) {
					extendWithBasicBlock(new RegularBlockImpl());
				}
				currentBlock.addStatement(node);
				return node;
			}
		}

		/**
		 * Extend the CFG with a regular node (not a conditional node), even
		 * though the helper is currently in the conditional mode. Can only be
		 * called when <code>conditionalMode</code> is true.
		 * 
		 * @param node
		 *            The node to add.
		 * @return The basic block the node has been added to.
		 */
		protected RegularBlockImpl extendWithNodeInConditionalMode(Node node) {
			assert conditionalMode;
			conditionalMode = false;
			extendWithNode(node);
			RegularBlockImpl trueBlock = finishCurrentBlock();
			conditionalMode = true;
			return trueBlock;
		}

		/**
		 * Extend the CFG with the basic block <code>bb</code>.
		 */
		protected void extendWithBasicBlock(BlockImpl bb) {
			if (currentBlock != null) {
				currentBlock.setSuccessor(bb);
			} else {
				truePredecessors.setSuccessorAs(bb);
				falsePredecessors.setSuccessorAs(bb);
				clearAnyPredecessor();
			}

			clearAnyPredecessor();
			if (bb.getType() == BlockType.REGULAR_BLOCK) {
				currentBlock = (RegularBlockImpl) bb;
			} else if (bb instanceof SingleSuccessorBlockImpl) {
				setSingleAnyPredecessor((SingleSuccessorBlockImpl) bb);
			} else {
				assert bb.getType() == BlockType.CONDITIONAL_BLOCK;
				ConditionalBlockImpl cb = (ConditionalBlockImpl) bb;
				setThenAsTruePredecessor(cb);
				setElseAsFalsePredecessor(cb);
			}
		}

		/**
		 * Finish the current basic block to not allow further additions to that
		 * block. Can only be called when there is a current block.
		 * 
		 * @return The basic block just finished.
		 */
		protected RegularBlockImpl finishCurrentBlock() {
			assert currentBlock != null;
			assert truePredecessors == null && falsePredecessors == null;
			RegularBlockImpl b = currentBlock;
			setSingleAnyPredecessor(currentBlock);
			return b;
		}

		/**
		 * Extend the CFG by a node, where <code>node</code> might throw any of
		 * the exception in <code>causes</code>.
		 * 
		 * @param node
		 *            The node to add.
		 * @param causes
		 *            Set of exceptions that the node might throw.
		 * @return The same node (for convenience).
		 */
		protected Node addToCurrentBlockWithException(Node node,
				Set<Class<? extends Throwable>> causes) {
			// TODO: make this method more general
			// make sure that 'node' gets its own basic block so that the
			// exception linking is correct
			if (!NodeUtils.isBooleanTypeNode(node)) {
				extendWithBasicBlock(new RegularBlockImpl());
			} else {
				// in this case, addToCurrentBlock will create a new block for
				// 'node'
			}

			extendWithNode(node);

			// add exceptional edges
			// TODO: catch clauses, finally, ...
			for (Class<? extends Throwable> c : causes) {
				currentBlock.addExceptionalSuccessor(exceptionalExitBlock, c);
			}

			finishCurrentBlock();

			return node;
		}

		/**
		 * Helper with just one cause, see
		 * <code>addToCurrentBlockWithException</code> for details.
		 */
		protected Node addToCurrentBlockWithException(Node node,
				Class<? extends Throwable> cause) {
			Set<Class<? extends Throwable>> causes = new HashSet<>();
			causes.add(cause);
			return addToCurrentBlockWithException(node, causes);
		}

		/* --------------------------------------------------------- */
		/* Visitor Methods */
		/* --------------------------------------------------------- */

		@Override
		public Node visitAnnotatedType(AnnotatedTypeTree tree, Void p) {
			assert false : "AnnotatedTypeTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitAnnotation(AnnotationTree tree, Void p) {
			assert false : "AnnotationTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitMethodInvocation(MethodInvocationTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitAssert(AssertTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitAssignment(AssignmentTree tree, Void p) {

			// see JLS 15.26.1

			assert !conditionalMode;

			Node expression;
			ExpressionTree variable = tree.getVariable();

			// case 1: field access
			if (ASTUtils.isFieldAccess(variable)) {
				// visit receiver
				Node receiver = getReceiver(variable);

				// visit expression
				expression = tree.getExpression().accept(this, p);

				// visit field access (throws null-pointer exception)
				FieldAccessNode target = new FieldAccessNode(variable, receiver);
				// TODO: static field access does not throw exception
				boolean canThrow = !(receiver instanceof ImplicitThisLiteralNode);
				// TODO: explicit this access does not throw exception
				if (canThrow) {
					addToCurrentBlockWithException(target,
							NullPointerException.class);
				} else {
					extendWithNode(target);
				}

				// add assignment node
				AssignmentNode assignmentNode = new AssignmentNode(tree,
						target, expression);
				extendWithNode(assignmentNode);
			}

			// TODO: case 2: array access

			// case 3: other cases
			else {
				Node target = variable.accept(this, p);
				expression = translateAssignment(tree, target,
						tree.getExpression());
			}
			return expression;
		}

		/**
		 * Translate an assignment.
		 */
		protected Node translateAssignment(Tree tree, Node target,
				ExpressionTree rhs) {
			assert tree instanceof AssignmentTree
					|| tree instanceof VariableTree;
			Node expression;
			expression = rhs.accept(this, null);
			AssignmentNode assignmentNode = new AssignmentNode(tree, target,
					expression);
			extendWithNode(assignmentNode);
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
				extendWithNode(node);
				return node;
			}
		}

		@Override
		public Node visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitBinary(BinaryTree tree, Void p) {
			// TODO: remaining binary node types
			Node r = null;
			switch (tree.getKind()) {
			case CONDITIONAL_OR: {

				// see JLS 15.24

				boolean condMode = conditionalMode;
				conditionalMode = true;

				// left-hand side
				Node left = tree.getLeftOperand().accept(this, p);
				PredecessorBlockHolder leftOutTrue = getAndResetTruePredecessors();
				PredecessorBlockHolder leftOutFalse = getAndResetFalsePredecessors();

				// right-hand side
				setAnyPredecessor(leftOutFalse);
				Node right = tree.getRightOperand().accept(this, p);
				PredecessorBlockHolder rightOutTrue = getAndResetTruePredecessors();
				PredecessorBlockHolder rightOutFalse = getAndResetFalsePredecessors();

				conditionalMode = condMode;

				if (conditionalMode) {
					// node for true case
					setAnyPredecessor(leftOutTrue);
					addAnyPredecessor(rightOutTrue);
					Node trueNode = new ConditionalOrNode(tree, left, right,
							true);
					RegularBlockImpl trueBlock = extendWithNodeInConditionalMode(trueNode);

					// node for false case
					setAnyPredecessor(rightOutFalse);
					Node falseNode = new ConditionalOrNode(tree, left, right,
							false);
					RegularBlockImpl falseBlock = extendWithNodeInConditionalMode(falseNode);

					setSingleTruePredecessor(trueBlock);
					setSingleFalsePredecessor(falseBlock);
					return trueNode;
				} else {
					// one node for true/false
					setAnyPredecessor(leftOutTrue);
					addAnyPredecessor(rightOutTrue);
					addAnyPredecessor(rightOutFalse);
					Node node = new ConditionalOrNode(tree, left, right, null);
					extendWithNode(node);
					return node;
				}
			}

			case EQUAL_TO: {

				// see JLS 15.21
				
				boolean cm = conditionalMode;
				conditionalMode = false;

				// left-hand side
				Node left = tree.getLeftOperand().accept(this, p);

				// right-hand side
				Node right = tree.getRightOperand().accept(this, p);
				
				conditionalMode = cm;
				
				// comparison
				EqualToNode node = new EqualToNode(tree, left, right);
				return extendWithNode(node);
			}
			}
			assert r != null : "unexpected binary tree";
			return extendWithNode(r);
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
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCase(CaseTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCatch(CatchTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitClass(ClassTree tree, Void p) {
			assert false : "ClassTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitConditionalExpression(ConditionalExpressionTree tree,
				Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitContinue(ContinueTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitErroneous(ErroneousTree tree, Void p) {
			assert false : "ErroneousTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitExpressionStatement(ExpressionStatementTree tree,
				Void p) {
			return tree.getExpression().accept(this, p);
		}

		@Override
		public Node visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
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
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitIdentifier(IdentifierTree tree, Void p) {
			// TODO: these are not always local variables
			LocalVariableNode node = new LocalVariableNode(tree);
			return extendWithNode(node);
		}

		@Override
		public Node visitIf(IfTree tree, Void p) {

			assert conditionalMode == false;

			// TODO exceptions
			PredecessorBlockHolder newPredecessors = null;

			// basic block for the condition
			conditionalMode = true;
			tree.getCondition().accept(this, null);
			conditionalMode = false;
			PredecessorBlockHolder trueOut = getAndResetTruePredecessors();
			PredecessorBlockHolder falseOut = getAndResetFalsePredecessors();

			// then branch
			setAnyPredecessor(trueOut);
			StatementTree thenStatement = tree.getThenStatement();
			thenStatement.accept(this, null);
			newPredecessors = addCurrentPredecessorToPredecessor(newPredecessors);

			// else branch
			StatementTree elseStatement = tree.getElseStatement();
			if (elseStatement != null) {
				setAnyPredecessor(falseOut);
				elseStatement.accept(this, null);
				newPredecessors = addCurrentPredecessorToPredecessor(newPredecessors);
			} else {
				// directly link the 'false' outgoing edge to the end
				newPredecessors = combinePredecessors(newPredecessors, falseOut);
			}

			setAnyPredecessor(newPredecessors);
			return null;
		}

		@Override
		public Node visitImport(ImportTree tree, Void p) {
			assert false : "ImportTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitArrayAccess(ArrayAccessTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitLabeledStatement(LabeledStatementTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
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
			return extendWithNode(r);
		}

		@Override
		public Node visitMethod(MethodTree tree, Void p) {
			assert false : "MethodTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitModifiers(ModifiersTree tree, Void p) {
			assert false : "ModifiersTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitNewArray(NewArrayTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitNewClass(NewClassTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitParenthesized(ParenthesizedTree tree, Void p) {
			return tree.getExpression().accept(this, p);
		}

		@Override
		public Node visitReturn(ReturnTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitMemberSelect(MemberSelectTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		protected Node translateFieldAccess(Tree tree) {
			assert ASTUtils.isFieldAccess(tree);
			// visit receiver
			Node receiver = getReceiver(tree);

			// visit field access (throws null-pointer exception)
			// TODO: exception
			FieldAccessNode access = new FieldAccessNode(tree, receiver);
			extendWithNode(access);
			return access;
		}

		@Override
		public Node visitEmptyStatement(EmptyStatementTree tree, Void p) {
			return null;
		}

		@Override
		public Node visitSwitch(SwitchTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitSynchronized(SynchronizedTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitThrow(ThrowTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitCompilationUnit(CompilationUnitTree tree, Void p) {
			assert false : "CompilationUnitTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTry(TryTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitParameterizedType(ParameterizedTypeTree tree, Void p) {
			assert false : "ParameterizedTypeTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitUnionType(UnionTypeTree tree, Void p) {
			assert false : "UnionTypeTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitArrayType(ArrayTypeTree tree, Void p) {
			assert false : "ArrayTypeTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTypeCast(TypeCastTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitPrimitiveType(PrimitiveTypeTree tree, Void p) {
			assert false : "PrimitiveTypeTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitTypeParameter(TypeParameterTree tree, Void p) {
			assert false : "TypeParameterTree is unexpected in AST to CFG translation";
			return null;
		}

		@Override
		public Node visitInstanceOf(InstanceOfTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitUnary(UnaryTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitVariable(VariableTree tree, Void p) {

			// see JLS 14.4

			// local variable definition
			extendWithNode(new VariableDeclarationNode(tree));

			// initializer
			Node node = null;
			ExpressionTree initializer = tree.getInitializer();
			if (initializer != null) {
				node = translateAssignment(tree, new LocalVariableNode(tree),
						initializer);
			}

			return node;
		}

		@Override
		public Node visitWhileLoop(WhileLoopTree tree, Void p) {
			assert false; // TODO Auto-generated method stub
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

		@Override
		public Node visitLambdaExpression(LambdaExpressionTree node, Void p) {
			assert false : "Lambda expressions not yet handled in AST to CFG translation.";
			return null;
		}

		@Override
		public Node visitMemberReference(MemberReferenceTree node, Void p) {
			assert false : "Member references not yet handled in AST to CFG translation.";
			return null;
		}
	}
}
