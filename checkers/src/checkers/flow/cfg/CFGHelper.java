package checkers.flow.cfg;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import checkers.flow.cfg.SpecialBasicBlockImpl.SpecialBasicBlockTypes;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.BooleanLiteralNode;
import checkers.flow.cfg.node.ConditionalOrNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.IdentifierNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
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
class CFGHelper implements TreeVisitor<Node, Void> {

	/**
	 * The basic block that is currently being filled with contents.
	 * 
	 * <p>
	 * 
	 * <strong>Important:</strong> Contents should be added through
	 * {@link addToCurrentBlock} instead of directly accessing
	 * {@link currentBlock}. Furthermore, the following invariant holds during
	 * CFG construction:
	 * 
	 * <pre>
	 * currentBlock == null  <==>  predecessors != null
	 * </pre>
	 * 
	 * <code>currentBlock</code> can be <code>null</code> to indicate that there
	 * is currently not a single current block, but rather a set of
	 * predecessors. This happens, for instance after an if-block where the then
	 * and else branch are the predecessors. If <code>currentBlock</code> is
	 * <code>null</code>, then adding a block will make all blocks in
	 * <code>predecessors</code> a predecessor of the newly created block.
	 * 
	 * Note that for conditional basic block in <code>predecessors</code>, the
	 * following block will be added as the 'then' successor.
	 */
	protected BasicBlockImpl currentBlock;

	/**
	 * Predecessors, details see <code>currentBlock</code>.
	 */
	protected PredecessorBlockHolder predecessors;

	/**
	 * The exceptional exit basic block (which might or might not be used).
	 */
	protected SpecialBasicBlockImpl exceptionalExitBlock;

	// TODO: docu
	// conditionalMode ==> currentBlock == null
	// conditionalMode ==> (truePredecessors != null && falsePredecessors !=
	// null) || predecessors != null (?)
	// in conditionalMode, the visit methods return null and use x instead
	protected boolean conditionalMode;
	protected PredecessorBlockHolder truePredecessors;
	protected PredecessorBlockHolder falsePredecessors;

	protected abstract class PredecessorBlockHolder {
		abstract public void setSuccessorAs(BasicBlock b);

		abstract public List<String> componentList();

		@Override
		public String toString() {
			return componentList().toString();
		}
	}

	protected PredecessorBlockHolder getAndResetFalsePredecessors() {
		PredecessorBlockHolder old = falsePredecessors;
		falsePredecessors = null;
		return old;
	}

	protected PredecessorBlockHolder getAndResetTruePredecessors() {
		PredecessorBlockHolder old = truePredecessors;
		truePredecessors = null;
		return old;
	}

	protected void setTruePredecessor(final BasicBlockImpl bb) {
		assert truePredecessors == null;
		assert !(bb instanceof ConditionalBasicBlockImpl);
		truePredecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				bb.setSuccessor(b);
			}

			@Override
			public List<String> componentList() {
				return Collections.singletonList(bb.toString());
			}
		};
	}

	protected void setFalsePredecessor(final BasicBlockImpl bb) {
		assert falsePredecessors == null;
		assert !(bb instanceof ConditionalBasicBlockImpl);
		falsePredecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				bb.setSuccessor(b);
			}

			@Override
			public List<String> componentList() {
				return Collections.singletonList(bb.toString());
			}
		};
	}

	protected void setThenAsTruePredecessor(final ConditionalBasicBlockImpl cb) {
		assert truePredecessors == null;
		truePredecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				cb.setThenSuccessor(b);
			}

			@Override
			public List<String> componentList() {
				return Collections.singletonList(cb.toString() + "<then>");
			}
		};
	}

	protected void setElseAsFalsePredecessor(final ConditionalBasicBlockImpl cb) {
		assert falsePredecessors == null;
		falsePredecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				cb.setElseSuccessor(b);
			}

			@Override
			public List<String> componentList() {
				return Collections.singletonList(cb.toString() + "<else>");
			}
		};
	}

	protected void setSinglePredecessor(final BasicBlockImpl bb) {
		assert predecessors == null;
		assert conditionalMode == false;
		predecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				bb.setSuccessor(b);
			}

			@Override
			public List<String> componentList() {
				return Collections.singletonList(bb.toString());
			}
		};
	}

	protected void addPredecessor(final PredecessorBlockHolder more) {
		final PredecessorBlockHolder old = predecessors;
		predecessors = new PredecessorBlockHolder() {
			@Override
			public void setSuccessorAs(BasicBlock b) {
				if (old != null) {
					old.setSuccessorAs(b);
				}
				if (more != null) {
					more.setSuccessorAs(b);
				}
			}

			@Override
			public List<String> componentList() {
				List<String> l = new LinkedList<>(old.componentList());
				l.addAll(more.componentList());
				return l;
			}
		};
	}

	/**
	 * Build the control flow graph for a {@link BlockTree} that represents a
	 * methods body.
	 * 
	 * @param t
	 *            Method body.
	 * @return The entry node of the resulting control flow graph.
	 */
	public BasicBlock build(BlockTree t) {

		// start in regular mode
		conditionalMode = false;

		// create start block
		BasicBlockImpl startBlock = new SpecialBasicBlockImpl(
				SpecialBasicBlockTypes.ENTRY);
		currentBlock = null;
		setSinglePredecessor(startBlock);

		// create exceptional end block
		exceptionalExitBlock = new SpecialBasicBlockImpl(
				SpecialBasicBlockTypes.EXCEPTIONAL_EXIT);

		// traverse AST
		t.accept(this, null);

		// finish CFG
		SpecialBasicBlockImpl exit = new SpecialBasicBlockImpl(
				SpecialBasicBlockTypes.EXIT);
		extendWithBasicBlock(exit);
		return startBlock;
	}

	/**
	 * Add the current block to a predecessor list (or the predecessors if
	 * currentBlock is null).
	 * 
	 * Note: newPredecessors can also be null.
	 */
	protected PredecessorBlockHolder addCurrentBlockAsPredecessor(
			final PredecessorBlockHolder newPredecessors) {
		assert !conditionalMode;
		if (currentBlock != null) {
			final BasicBlockImpl cb = currentBlock;
			return new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BasicBlock b) {
					if (newPredecessors != null) {
						newPredecessors.setSuccessorAs(b);
					}
					cb.setSuccessor(b);
				}

				@Override
				public List<String> componentList() {
					List<String> l = new LinkedList<>();
					if (newPredecessors != null) {
						l.addAll(newPredecessors.componentList());
					}
					l.add(cb.toString());
					return l;
				}
			};
		} else {
			final PredecessorBlockHolder o = predecessors;
			return new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BasicBlock b) {
					if (newPredecessors != null) {
						newPredecessors.setSuccessorAs(b);
					}
					o.setSuccessorAs(b);
				}

				@Override
				public List<String> componentList() {
					List<String> l = new LinkedList<>(o.componentList());
					if (newPredecessors != null) {
						l.addAll(newPredecessors.componentList());
					}
					return l;
				}
			};
		}
	}

	/**
	 * Add a node to the current basic block, correctly linking all blocks and
	 * handling conditional basic block appropriately.
	 * 
	 * @param node
	 *            The node to add.
	 * @return The same node (for convenience).
	 */
	protected Node extendWithNode(Node node) {
		if (conditionalMode) {
			extendWithConditionalNode(node);
			return node;
		}
		if (currentBlock == null) {
			extendWithBasicBlock(new BasicBlockImpl());
		}
		currentBlock.addStatement(node);
		return node;
	}

	// TODO: docu + check docu of other method above
	// note: does not set 'currentblock' or
	// addThenPredecessor((ConditionalBasicBlockImpl) bb);

	// only in conditional mode we add conditional nodes, but possibly also
	// normal blocks
	protected ConditionalBasicBlockImpl extendWithConditionalNode(Node node) {
		assert conditionalMode;
		ConditionalBasicBlockImpl cb = new ConditionalBasicBlockImpl();
		cb.setCondition(node);
		extendWithBasicBlock(cb);
		return cb;
	}

	protected BasicBlockImpl extendWithNodeInConditionalMode(Node node) {
		assert conditionalMode;
		conditionalMode = false;
		extendWithNode(node);
		BasicBlockImpl trueBlock = finishCurrentBlock();
		conditionalMode = true;
		return trueBlock;
	}

	/**
	 * Extend the control flow graph with <code>bb</code>.
	 */
	protected void extendWithBasicBlock(BasicBlockImpl bb) {
		assert conditionalMode ? bb instanceof ConditionalBasicBlockImpl : true;

		if (currentBlock != null) {
			currentBlock.setSuccessor(bb);
		} else {
			predecessors.setSuccessorAs(bb);
			predecessors = null;
		}
		if (conditionalMode) {
			currentBlock = null;
		} else {
			currentBlock = bb;
		}
	}

	// TODO: docu
	protected BasicBlockImpl finishCurrentBlock() {
		assert currentBlock != null;
		assert truePredecessors == null && falsePredecessors == null;
		assert !conditionalMode;
		BasicBlockImpl b = currentBlock;
		setSinglePredecessor(currentBlock);
		currentBlock = null;
		return b;
	}

	/**
	 * Add a node to the current basic block, where <code>node</code> might
	 * throw any of the exception in <code>causes</code>.
	 * 
	 * @param node
	 *            The node to add.
	 * @param causes
	 *            Set of exceptions that the node might throw.
	 * @return The same node (for convenience).
	 */
	protected Node addToCurrentBlockWithException(Node node,
			Set<Class<?>> causes) {
		// make sure that 'node' gets its own basic block so that the
		// exception linking is correct
		if (!NodeUtils.isBooleanTypeNode(node)
				&& !(currentBlock instanceof ConditionalBasicBlockImpl)) {
			extendWithBasicBlock(new BasicBlockImpl());
		} else {
			// in this case, addToCurrentBlock will create a new block for
			// 'node'
		}

		extendWithNode(node);

		// add exceptional edges
		// TODO: catch clauses, finally, ...
		for (Class<?> c : causes) {
			currentBlock.addExceptionalSuccessor(exceptionalExitBlock, c);
		}

		finishCurrentBlock();

		return node;
	}

	/**
	 * Helper with just one cause, see
	 * <code>addToCurrentBlockWithException</code> for details.
	 */
	protected Node addToCurrentBlockWithException(Node node, Class<?> cause) {
		Set<Class<?>> causes = new HashSet<>();
		causes.add(cause);
		return addToCurrentBlockWithException(node, causes);
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

		Node expression;
		ExpressionTree variable = tree.getVariable();

		// case 1: field access
		if (ASTUtils.isFieldAccess(variable)) {
			// visit receiver
			Node receiver = getReceiver(variable);

			// visit expression
			expression = tree.getExpression().accept(this, p);

			// visit field access (throws null-pointer exception)
			String field = ASTUtils.getFieldName(variable);
			FieldAccessNode target = new FieldAccessNode(variable, receiver,
					field);
			// TODO: static field access does not throw exception
			addToCurrentBlockWithException(target, NullPointerException.class);

			// add assignment node
			AssignmentNode assignmentNode = new AssignmentNode(tree, target,
					expression);
			extendWithNode(assignmentNode);
		}

		// TODO: case 2: array access

		// case 3: other cases
		else {
			Node target = variable.accept(this, p);
			expression = translateAssignment(tree, target, tree.getExpression());
		}
		return expression;
	}

	/**
	 * Translate an assignment.
	 */
	protected Node translateAssignment(Tree tree, Node target,
			ExpressionTree rhs) {
		assert tree instanceof AssignmentTree || tree instanceof VariableTree;
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
		case CONDITIONAL_OR:

			// see JLS 15.24
			
			boolean condMode = conditionalMode;
			conditionalMode = true;

			// left-hand side
			Node left = tree.getLeftOperand().accept(this, p);
			PredecessorBlockHolder leftOutTrue = getAndResetTruePredecessors();
			PredecessorBlockHolder leftOutFalse = getAndResetFalsePredecessors();

			// right-hand side
			predecessors = leftOutFalse;
			Node right = tree.getRightOperand().accept(this, p);
			PredecessorBlockHolder rightOutTrue = getAndResetTruePredecessors();
			PredecessorBlockHolder rightOutFalse = getAndResetFalsePredecessors();
			
			conditionalMode = condMode;

			if (conditionalMode) {
				// node for true case
				predecessors = leftOutTrue;
				addPredecessor(rightOutTrue);
				Node trueNode = new ConditionalOrNode(tree, left, right, true);
				BasicBlockImpl trueBlock = extendWithNodeInConditionalMode(trueNode);

				// node for false case
				predecessors = rightOutFalse;
				Node falseNode = new ConditionalOrNode(tree, left, right, false);
				BasicBlockImpl falseBlock = extendWithNodeInConditionalMode(falseNode);

				predecessors = null;
				setTruePredecessor(trueBlock);
				setFalsePredecessor(falseBlock);
				return trueNode;
			} else {
				// one node for true/false
				predecessors = leftOutTrue;
				addPredecessor(rightOutTrue);
				addPredecessor(rightOutFalse);
				Node node = new ConditionalOrNode(tree, left, right, null);
				extendWithNode(node);
				return node;
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
		assert false : "WildcardTree is unexpected in AST to CFG translation";
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
		assert false : "WildcardTree is unexpected in AST to CFG translation";
		return null;
	}

	@Override
	public Node visitExpressionStatement(ExpressionStatementTree tree, Void p) {
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
		 * BasicBlockImplementation initBlock = new BasicBlockImplementation();
		 * ConditionalBasicBlockImplementation conditionBlock = new
		 * ConditionalBasicBlockImplementation(); BasicBlockImplementation
		 * afterBlock = new BasicBlockImplementation(); BasicBlockImplementation
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
		 * currentBlock = loopBodyBlock; tree.getStatement().accept(this, null);
		 * for (StatementTree t : tree.getUpdate()) { t.accept(this, null); }
		 * currentBlock.addSuccessor(conditionBlock);
		 * 
		 * currentBlock = afterBlock;
		 */
		assert false; // TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node visitIdentifier(IdentifierTree tree, Void p) {
		IdentifierNode node = new IdentifierNode(tree);
		if (conditionalMode) {
			ConditionalBasicBlockImpl cb = extendWithConditionalNode(node);
			setThenAsTruePredecessor(cb);
			setElseAsFalsePredecessor(cb);
			return node;
		} else {
			return extendWithNode(node);
		}
	}

	@Override
	public Node visitIf(IfTree tree, Void p) {

		assert conditionalMode == false;

		// TODO exceptions
		PredecessorBlockHolder newPredecessors = null;

		// basic block for the condition
		// TODO make method switchToConditionalMode
		if (currentBlock != null) {
			finishCurrentBlock();
		}
		conditionalMode = true;
		tree.getCondition().accept(this, null);
		conditionalMode = false;
		PredecessorBlockHolder trueOut = getAndResetTruePredecessors();
		final PredecessorBlockHolder falseOut = getAndResetFalsePredecessors();

		// then branch
		assert currentBlock == null;
		predecessors = trueOut;
		StatementTree thenStatement = tree.getThenStatement();
		thenStatement.accept(this, null);
		newPredecessors = addCurrentBlockAsPredecessor(newPredecessors);
		currentBlock = null;
		predecessors = null;

		// else branch
		StatementTree elseStatement = tree.getElseStatement();
		if (elseStatement != null) {
			assert currentBlock == null;
			predecessors = falseOut;
			elseStatement.accept(this, null);
			newPredecessors = addCurrentBlockAsPredecessor(newPredecessors);
		} else {
			// directly link the 'false' outgoing edge to the end
			final PredecessorBlockHolder np = newPredecessors;
			newPredecessors = new PredecessorBlockHolder() {
				@Override
				public void setSuccessorAs(BasicBlock b) {
					np.setSuccessorAs(b);
					falseOut.setSuccessorAs(b);
				}

				@Override
				public List<String> componentList() {
					List<String> l = new LinkedList<>(np.componentList());
					l.addAll(falseOut.componentList());
					return l;
				}
			};
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
		String field = ASTUtils.getFieldName(tree);
		FieldAccessNode access = new FieldAccessNode(tree, receiver, field);
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
		assert false : "WildcardTree is unexpected in AST to CFG translation";
		return null;
	}

	@Override
	public Node visitTry(TryTree tree, Void p) {
		assert false; // TODO Auto-generated method stub
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
		assert false; // TODO Auto-generated method stub
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
			node = translateAssignment(tree, new IdentifierNode(tree),
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

}