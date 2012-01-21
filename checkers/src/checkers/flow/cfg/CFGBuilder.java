package checkers.flow.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import checkers.flow.cfg.CFGBuilder.ExtendedNode.ExtendedNodeType;
import checkers.flow.cfg.block.BlockImpl;
import checkers.flow.cfg.block.ConditionalBlockImpl;
import checkers.flow.cfg.block.RegularBlockImpl;
import checkers.flow.cfg.block.SingleSuccessorBlockImpl;
import checkers.flow.cfg.block.SpecialBlock.SpecialBlockType;
import checkers.flow.cfg.block.SpecialBlockImpl;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.BooleanLiteralNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.VariableDeclarationNode;
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
		return new CFGPhaseTwo().process(new CFGPhaseOne().process(method));
	}

	protected static class ExtendedNode {
		protected BlockImpl block;
		protected ExtendedNodeType type;

		public ExtendedNode(ExtendedNodeType type) {
			this.type = type;
		}

		public enum ExtendedNodeType {
			NODE, CONDITIONAL_MARKER, JUMP_MARKER, EXCEPTION_NODE, REGULAR_EXIT_JUMP, EXCEPTIONAL_EXIT_JUMP
		}

		public ExtendedNodeType getType() {
			return type;
		}

		public Node getNode() {
			assert false;
			return null;
		}

		public Label getLabel() {
			assert false;
			return null;
		}

		public BlockImpl getBlock() {
			return block;
		}

		public void setBlock(BlockImpl b) {
			this.block = b;
		}
		
		@Override
		public String toString() {
			return "ExtendedNode("+type+")";
		}
	}

	protected static class NodeHolder extends ExtendedNode {

		protected Node node;

		public NodeHolder(Node node) {
			super(ExtendedNodeType.NODE);
			this.node = node;
		}

		@Override
		public Node getNode() {
			return node;
		}
		
		@Override
		public String toString() {
			return "NodeHolder("+node+")";
		}

	}

	protected static class ConditionalMarker extends ExtendedNode {

		private Label falseSucc;

		public ConditionalMarker(Label falseSucc) {
			super(ExtendedNodeType.CONDITIONAL_MARKER);
			this.falseSucc = falseSucc;
		}

		@Override
		public Label getLabel() {
			return falseSucc;
		}
		
		@Override
		public String toString() {
			return "ConditionalMarker("+getLabel()+")";
		}
	}

	protected static class JumpMarker extends ExtendedNode {
		private Label jumpTarget;

		public JumpMarker(Label jumpTarget) {
			super(ExtendedNodeType.JUMP_MARKER);
			this.jumpTarget = jumpTarget;
		}

		@Override
		public Label getLabel() {
			return jumpTarget;
		}
		
		@Override
		public String toString() {
			return "JumpMarker("+getLabel()+")";
		}
	}

	protected static class PhaseOneResult {

		private IdentityHashMap<Tree, Node> treeLookupMap;
		private MethodTree tree;
		private Map<ExtendedNode, Label> references;
		private Map<Label, Integer> bindings;
		private ArrayList<ExtendedNode> nodeList;
		private Set<Integer> leaders;

		public PhaseOneResult(MethodTree t,
				IdentityHashMap<Tree, Node> treeLookupMap,
				ArrayList<ExtendedNode> nodeList, Map<Label, Integer> bindings,
				Map<ExtendedNode, Label> references, Set<Integer> leaders) {
			this.tree = t;
			this.treeLookupMap = treeLookupMap;
			this.nodeList = nodeList;
			this.bindings = bindings;
			this.references = references;
			this.leaders = leaders;
		}

	}

	protected static class Label {

	}

	protected static class CFGPhaseTwo {

		protected Map<SingleSuccessorBlockImpl, Integer> missingEdges;

		SpecialBlockImpl regularExitBlock;
		SpecialBlockImpl exceptionalExitBlock;

		ControlFlowGraph process(PhaseOneResult in) {

			Map<Label, Integer> bindings = in.bindings;
			ArrayList<ExtendedNode> nodeList = in.nodeList;
			Map<ExtendedNode, Label> references = in.references;
			Set<Integer> leaders = in.leaders;

			assert in.nodeList.size() > 0;

			// exit blocks
			regularExitBlock = new SpecialBlockImpl(SpecialBlockType.EXIT);
			exceptionalExitBlock = new SpecialBlockImpl(
					SpecialBlockType.EXCEPTIONAL_EXIT);

			// no missing edges yet
			missingEdges = new HashMap<>();

			// create start block
			SpecialBlockImpl startBlock = new SpecialBlockImpl(
					SpecialBlockType.ENTRY);
			missingEdges.put(startBlock, 0);

			// loop through all 'leaders' (while dynamically detecting the
			// leaders)
			RegularBlockImpl block = new RegularBlockImpl();
			int i = 0;
			for (ExtendedNode node : nodeList) {
				switch (node.getType()) {
				case NODE:
					if (leaders.contains(i)) {
						RegularBlockImpl b = new RegularBlockImpl();
						block.setSuccessor(b);
						block = b;
					}
					block.addNode(node.getNode());
					node.setBlock(block);
					break;
				case CONDITIONAL_MARKER:
					// no label is supposed to point to conditional marker
					// nodes, thus we do not need to set block for 'node'
					assert block != null;
					final ConditionalBlockImpl cb = new ConditionalBlockImpl();
					block.setSuccessor(cb);
					block = new RegularBlockImpl();
					// use two anonymous SingleSuccessorBlockImpl that set the
					// 'then' and 'else' successor of the conditional block
					missingEdges.put(new SingleSuccessorBlockImpl() {
						@Override
						public void setSuccessor(BlockImpl successor) {
							cb.setThenSuccessor(successor);
						}
					}, i + 1);
					missingEdges.put(new SingleSuccessorBlockImpl() {
						@Override
						public void setSuccessor(BlockImpl successor) {
							cb.setElseSuccessor(successor);
						}
					}, bindings.get(node.getLabel()));
					break;
				case JUMP_MARKER:
					node.setBlock(block);
					missingEdges.put(block, bindings.get(node.getLabel()));
					block = new RegularBlockImpl();
					break;
				case EXCEPTION_NODE:
					// TODO
					break;
				case REGULAR_EXIT_JUMP:
					block.setSuccessor(regularExitBlock);
					node.setBlock(block);
					block = new RegularBlockImpl();
					break;
				case EXCEPTIONAL_EXIT_JUMP:
					block.setSuccessor(exceptionalExitBlock);
					node.setBlock(block);
					block = new RegularBlockImpl();
					break;
				}
				i++;
			}

			// add missing edges
			for (Entry<SingleSuccessorBlockImpl, Integer> e : missingEdges
					.entrySet()) {
				ExtendedNode extendedNode = nodeList.get(e.getValue());
				BlockImpl target = extendedNode.getBlock();
				e.getKey().setSuccessor(target);
			}

			return new ControlFlowGraph(startBlock, in.tree, in.treeLookupMap);
		}
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
	protected static class CFGPhaseOne implements TreeVisitor<Node, Void> {

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

		/** Map from AST {@link Tree}s to {@link Node}s. */
		// TODO: fill this map with contents.
		protected IdentityHashMap<Tree, Node> treeLookupMap;

		protected ArrayList<ExtendedNode> nodeList;

		protected Map<Label, Integer> bindings;

		protected Map<ExtendedNode, Label> references;
		
		protected Set<Integer> leaders;

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
		public PhaseOneResult process(MethodTree t) {

			// start in regular mode
			conditionalMode = false;

			// initialize lists and maps
			treeLookupMap = new IdentityHashMap<>();
			nodeList = new ArrayList<>();
			bindings = new HashMap<>();
			references = new HashMap<>();
			leaders = new HashSet<>();

			// traverse AST of the method body
			t.getBody().accept(this, null);

			// add marker to indicate that the next block will be the exit block
			nodeList.add(new ExtendedNode(ExtendedNodeType.REGULAR_EXIT_JUMP));

			return new PhaseOneResult(t, treeLookupMap, nodeList, bindings,
					references, leaders);
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
			extendWithExtendedNode(new NodeHolder(node));
			return node;
		}

		protected void extendWithExtendedNode(ExtendedNode n) {
			if (n.getType() == ExtendedNodeType.CONDITIONAL_MARKER) {
				leaders.add(nodeList.size()-1);
			}
			nodeList.add(n);
		}
		
		protected void addLabelForNextNode(Label l) {
			leaders.add(nodeList.size());
			bindings.put(l, nodeList.size());
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
					// TODO: handle exceptions
					// addToCurrentBlockWithException(target,
					// NullPointerException.class);
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
			/*
			 * Node r = null; switch (tree.getKind()) { case CONDITIONAL_OR: {
			 * 
			 * // see JLS 15.24
			 * 
			 * boolean condMode = conditionalMode; conditionalMode = true;
			 * 
			 * // left-hand side Node left = tree.getLeftOperand().accept(this,
			 * p); PredecessorBlockHolder leftOutTrue =
			 * getAndResetTruePredecessors(); PredecessorBlockHolder
			 * leftOutFalse = getAndResetFalsePredecessors();
			 * 
			 * // right-hand side setAnyPredecessor(leftOutFalse); Node right =
			 * tree.getRightOperand().accept(this, p); PredecessorBlockHolder
			 * rightOutTrue = getAndResetTruePredecessors();
			 * PredecessorBlockHolder rightOutFalse =
			 * getAndResetFalsePredecessors();
			 * 
			 * conditionalMode = condMode;
			 * 
			 * if (conditionalMode) { // node for true case
			 * setAnyPredecessor(leftOutTrue); addAnyPredecessor(rightOutTrue);
			 * Node trueNode = new ConditionalOrNode(tree, left, right, true);
			 * SingleSuccessorBlockImpl trueBlock =
			 * extendWithNodeInConditionalMode(trueNode);
			 * 
			 * // node for false case setAnyPredecessor(rightOutFalse); Node
			 * falseNode = new ConditionalOrNode(tree, left, right, false);
			 * SingleSuccessorBlockImpl falseBlock =
			 * extendWithNodeInConditionalMode(falseNode);
			 * 
			 * setSingleTruePredecessor(trueBlock);
			 * setSingleFalsePredecessor(falseBlock); return trueNode; } else {
			 * // one node for true/false setAnyPredecessor(leftOutTrue);
			 * addAnyPredecessor(rightOutTrue);
			 * addAnyPredecessor(rightOutFalse); Node node = new
			 * ConditionalOrNode(tree, left, right, null); extendWithNode(node);
			 * return node; } }
			 * 
			 * case EQUAL_TO: {
			 * 
			 * // see JLS 15.21
			 * 
			 * boolean cm = conditionalMode; conditionalMode = false;
			 * 
			 * // left-hand side Node left = tree.getLeftOperand().accept(this,
			 * p);
			 * 
			 * // right-hand side Node right =
			 * tree.getRightOperand().accept(this, p);
			 * 
			 * conditionalMode = cm;
			 * 
			 * // comparison EqualToNode node = new EqualToNode(tree, left,
			 * right); return extendWithNode(node); } } assert r != null :
			 * "unexpected binary tree"; return extendWithNode(r);
			 */
			return null;
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

			// basic block for the condition
			conditionalMode = true;
			tree.getCondition().accept(this, null);
			conditionalMode = false;
			Label elseEntry = new Label();
			extendWithExtendedNode(new ConditionalMarker(elseEntry));

			// then branch
			Label endIf = new Label();
			StatementTree thenStatement = tree.getThenStatement();
			thenStatement.accept(this, null);
			extendWithExtendedNode(new JumpMarker(endIf));

			// else branch
			addLabelForNextNode(elseEntry);
			StatementTree elseStatement = tree.getElseStatement();
			if (elseStatement != null) {
				elseStatement.accept(this, null);
			}

			// label the end of the if statement
			addLabelForNextNode(endIf);

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
