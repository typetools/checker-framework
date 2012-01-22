package checkers.flow.cfg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import checkers.flow.cfg.block.Block;
import checkers.flow.cfg.block.Block.BlockType;
import checkers.flow.cfg.block.BlockImpl;
import checkers.flow.cfg.block.ConditionalBlockImpl;
import checkers.flow.cfg.block.ExceptionBlockImpl;
import checkers.flow.cfg.block.RegularBlockImpl;
import checkers.flow.cfg.block.SingleSuccessorBlockImpl;
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
 * <p>
 * 
 * The translation of the AST to the CFG is split into three phases:
 * <ol>
 * <li><em>Phase one.</em> In the first phase, the AST is translated into a
 * sequence of {@link ExtendedNode}s. An extended node can either be a
 * {@link Node}, or one of several meta elements such as a conditional or
 * unconditional jump or a node with additional information about exceptions.
 * Some of the extended nodes contain labels (e.g., for the jump target), and
 * phase one additionally creates a mapping from labels to extended nodes.
 * Finally, the list of leaders is computed: A leader is an extended node which
 * will give rise to a basic block in phase two.</li>
 * <li><em>Phase two.</em> In this phase, the sequence of extended nodes is
 * translated to a graph of control flow blocks that contain nodes. The meta
 * elements from phase one are translated into the correct edges.</li>
 * <li><em>Phase three.</em> In phase two, some basic blocks that are generated
 * might be empty. This phase removes empty basic blocks while preserving the
 * control flow structure.</li>
 * </ol>
 * 
 * @author Stefan Heule
 * 
 */
public class CFGBuilder {

	/**
	 * Build the control flow graph of a method.
	 */
	public static ControlFlowGraph build(MethodTree method) {
		PhaseOneResult phase1result = new CFGTranslationPhaseOne()
				.process(method);
		ControlFlowGraph phase2result = CFGTranslationPhaseTwo
				.process(phase1result);
		ControlFlowGraph phase3result = CFGTranslationPhaseThree
				.process(phase2result);
		return phase3result;
	}

	/* --------------------------------------------------------- */
	/* Extended Node Types and Labels */
	/* --------------------------------------------------------- */

	/** Special label to identify the exceptional exit. */
	protected static Label exceptionalExitLabel = new Label();

	/** Special label to identify the regular exit. */
	protected static Label regularExitLabel = new Label();

	/**
	 * An extended node can be one of several things (depending on its
	 * {@code type}):
	 * <ul>
	 * <li><em>NODE</em>. An extended node of this type is just a wrapper for a
	 * {@link Node} (that cannot throw exceptions).</li>
	 * <li><em>EXCEPTION_NODE</em>. A wrapper for a {@link Node} which can throw
	 * exceptions. It contains a label for every possible exception type the
	 * node might throw.</li>
	 * <li><em>CONDITIONAL_JUMP</em>. Marks that the previous extended node
	 * (which is either of type <em>NODE</em> or <em>EXCEPTION_NODE</em>) is the
	 * condition of a branch. Implicitly, the following extended node in the
	 * sequence will be the jump target if the condition evaluates to
	 * {@code true}. If it evaluates to {@code false}, then the control flow
	 * continues at a specific label (stored in this extended node).</li>
	 * <li><em>UNCONDITIONAL_JUMP</em>. An unconditional jump to a label.</li>
	 * </ul>
	 */
	protected static abstract class ExtendedNode {

		/**
		 * The basic block this extended node belongs to (as determined in phase
		 * two).
		 */
		protected BlockImpl block;

		/** Type of this node. */
		protected ExtendedNodeType type;

		public ExtendedNode(ExtendedNodeType type) {
			this.type = type;
		}

		/** Extended node types (description see above). */
		public enum ExtendedNodeType {
			NODE, EXCEPTION_NODE, CONDITIONAL_JUMP, UNCONDITIONAL_JUMP
		}

		public ExtendedNodeType getType() {
			return type;
		}

		/**
		 * @return The node contained in this extended node (only applicable if
		 *         the type is {@code NODE} or {@code EXCEPTION_NODE}).
		 */
		public Node getNode() {
			assert false;
			return null;
		}

		/**
		 * @return The label associated with this extended node (only applicable
		 *         if type is {@code CONDITIONAL_JUMP} or
		 *         {@link UNCONDITIONAL_JUMP}).
		 */
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
			return "ExtendedNode(" + type + ")";
		}
	}

	/**
	 * An extended node of type {@code NODE}.
	 */
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
			return "NodeHolder(" + node + ")";
		}

	}

	/**
	 * An extended node of type {@code EXCEPTION_NODE}.
	 */
	protected static class NodeWithExceptionsHolder extends ExtendedNode {

		protected Node node;
		protected Map<Class<? extends Throwable>, Label> exceptions;

		public NodeWithExceptionsHolder(Node node,
				Map<Class<? extends Throwable>, Label> exceptions) {
			super(ExtendedNodeType.EXCEPTION_NODE);
			this.node = node;
			this.exceptions = exceptions;
		}

		public Map<Class<? extends Throwable>, Label> getExceptions() {
			return exceptions;
		}

		@Override
		public Node getNode() {
			return node;
		}

		@Override
		public String toString() {
			return "NodeWithExceptionsHolder(" + node + ")";
		}

	}

	/**
	 * An extended node of type {@code CONDITIONAL_JUMP}.
	 */
	protected static class ConditionalJump extends ExtendedNode {

		protected Label falseSucc;

		public ConditionalJump(Label falseSucc) {
			super(ExtendedNodeType.CONDITIONAL_JUMP);
			this.falseSucc = falseSucc;
		}

		@Override
		public Label getLabel() {
			return falseSucc;
		}

		@Override
		public String toString() {
			return "ConditionalMarker(" + getLabel() + ")";
		}
	}

	/**
	 * An extended node of type {@code UNCONDITIONAL_JUMP}.
	 */
	protected static class UnconditionalJump extends ExtendedNode {

		protected Label jumpTarget;

		public UnconditionalJump(Label jumpTarget) {
			super(ExtendedNodeType.UNCONDITIONAL_JUMP);
			this.jumpTarget = jumpTarget;
		}

		@Override
		public Label getLabel() {
			return jumpTarget;
		}

		@Override
		public String toString() {
			return "JumpMarker(" + getLabel() + ")";
		}
	}

	/**
	 * A label that can be used to refer to other extended nodes.
	 */
	protected static class Label {
	}

	/* --------------------------------------------------------- */
	/* Phase Three */
	/* --------------------------------------------------------- */

	/**
	 * Class that performs phase three of the translation process.
	 */
	protected static class CFGTranslationPhaseThree {

		/**
		 * A simple wrapper object that holds a basic block and allows to set
		 * one of its successors.
		 */
		protected interface PredecessorHolder {
			void setSuccessor(BlockImpl b);
		}

		/**
		 * Perform phase three on the control flow graph {@code cfg}.
		 * 
		 * @param cfg
		 *            The control flow graph. Ownership is transfered to this
		 *            method and the caller is not allowed to read or modify
		 *            {@code cfg} after the call to {@code process} any more.
		 * @return The resulting control flow graph.
		 */
		@SuppressWarnings("unchecked")
		public static ControlFlowGraph process(ControlFlowGraph cfg) {
			Set<Block> worklist = cfg.getAllBlocks();
			Set<Block> dontVisit = new HashSet<>();

			// remove empty blocks
			for (Block cur : worklist) {
				if (dontVisit.contains(cur)) {
					continue;
				}

				if (cur.getType() == BlockType.REGULAR_BLOCK) {
					RegularBlockImpl b = (RegularBlockImpl) cur;
					if (b.isEmpty()) {
						Set<RegularBlockImpl> empty = new HashSet<>();
						Set<PredecessorHolder> predecessors = new HashSet<>();
						BlockImpl succ = computeNeighborhoodOfEmptyBlock(b,
								empty, predecessors);
						dontVisit.addAll(empty);
						dontVisit
								.addAll((Collection<? extends Block>) predecessors);
						for (PredecessorHolder p : predecessors) {
							p.setSuccessor(succ);
						}
					}
				}
			}

			return cfg;
		}

		/**
		 * Compute the set of empty regular basic blocks {@code empty}, starting
		 * at {@code start} and going both forward and backwards. Furthermore,
		 * compute the predecessors of these empty blocks ({@code predecessors}
		 * ), and their single successor (return value).
		 * 
		 * @param start
		 *            The starting point of the search (an empty, regular basic
		 *            block).
		 * @param empty
		 *            An empty set to be filled by this method with all empty
		 *            basic blocks found (including {@code start}).
		 * @param predecessors
		 *            An empty set to be filled by this method with all
		 *            predecessors.
		 * @return The single successor of the set of the empty basic blocks.
		 */
		protected static BlockImpl computeNeighborhoodOfEmptyBlock(
				RegularBlockImpl start, Set<RegularBlockImpl> empty,
				Set<PredecessorHolder> predecessors) {

			// get empty neighborhood that come before 'start'
			computeNeighborhoodOfEmptyBlockBackwards(start, empty, predecessors);

			// go forward
			BlockImpl succ = (BlockImpl) start.getSuccessor();
			while (succ.getType() == BlockType.REGULAR_BLOCK) {
				RegularBlockImpl cur = (RegularBlockImpl) succ;
				if (cur.isEmpty()) {
					computeNeighborhoodOfEmptyBlockBackwards(cur, empty,
							predecessors);
					empty.add(cur);
					succ = (BlockImpl) cur.getSuccessor();
				} else {
					break;
				}
			}
			return succ;
		}

		/**
		 * Compute the set of empty regular basic blocks {@code empty}, starting
		 * at {@code start} and looking only backwards in the control flow
		 * graph. Furthermore, compute the predecessors of these empty blocks (
		 * {@code predecessors}).
		 * 
		 * @param start
		 *            The starting point of the search (an empty, regular basic
		 *            block).
		 * @param empty
		 *            A set to be filled by this method with all empty basic
		 *            blocks found (including {@code start}).
		 * @param predecessors
		 *            A set to be filled by this method with all predecessors.
		 */
		protected static void computeNeighborhoodOfEmptyBlockBackwards(
				RegularBlockImpl start, Set<RegularBlockImpl> empty,
				Set<PredecessorHolder> predecessors) {
			Queue<RegularBlockImpl> worklist = new LinkedList<>();
			worklist.add(start);
			while (!worklist.isEmpty()) {
				RegularBlockImpl cur = worklist.poll();
				empty.add(cur);
				for (final BlockImpl pred : cur.getPredecessors()) {
					switch (pred.getType()) {
					case SPECIAL_BLOCK:
						// add pred correctly to predecessor list
						SingleSuccessorBlockImpl s = (SingleSuccessorBlockImpl) pred;
						predecessors.add(singleSuccessorHolder(s));
						break;
					case CONDITIONAL_BLOCK:
						// add pred correctly to predecessor list
						final ConditionalBlockImpl c = (ConditionalBlockImpl) pred;
						if (c.getThenSuccessor() == cur) {
							predecessors.add(new PredecessorHolder() {
								@Override
								public void setSuccessor(BlockImpl b) {
									c.setThenSuccessor(b);
								}
							});
						} else {
							assert c.getElseSuccessor() == cur;
							predecessors.add(new PredecessorHolder() {
								@Override
								public void setSuccessor(BlockImpl b) {
									c.setElseSuccessor(b);
								}
							});
						}
						break;
					case EXCEPTION_BLOCK:
						// add pred correctly to predecessor list
						final ExceptionBlockImpl e = (ExceptionBlockImpl) pred;
						if (e.getSuccessor() == cur) {
							predecessors.add(singleSuccessorHolder(e));
						} else {
							Set<Entry<Class<? extends Throwable>, Block>> entrySet = e
									.getExceptionalSuccessors().entrySet();
							for (final Entry<Class<? extends Throwable>, Block> entry : entrySet) {
								if (entry.getValue() == cur) {
									predecessors.add(new PredecessorHolder() {
										@Override
										public void setSuccessor(BlockImpl b) {
											e.addExceptionalSuccessor(b,
													entry.getKey());
										}
									});
									break;
								}
							}
						}
						break;
					case REGULAR_BLOCK:
						RegularBlockImpl r = (RegularBlockImpl) pred;
						if (r.isEmpty()) {
							// recursively look backwards
							computeNeighborhoodOfEmptyBlockBackwards(r, empty,
									predecessors);
						} else {
							// add pred correctly to predecessor list
							predecessors.add(singleSuccessorHolder(r));
						}
						break;
					}
				}
			}
		}

		/**
		 * @return A {@link PredecessorHolder} that sets the successor of a
		 *         single successor block {@code s}.
		 */
		protected static PredecessorHolder singleSuccessorHolder(
				final SingleSuccessorBlockImpl s) {
			return new PredecessorHolder() {
				@Override
				public void setSuccessor(BlockImpl b) {
					s.setSuccessor(b);
				}
			};
		}
	}

	/* --------------------------------------------------------- */
	/* Phase Two */
	/* --------------------------------------------------------- */

	/** Tuple class with up to three members. */
	protected static class Tuple<A, B, C> {
		public A a;
		public B b;
		public C c;

		public Tuple(A a, B b) {
			this.a = a;
			this.b = b;
		}

		public Tuple(A a, B b, C c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	/**
	 * Class that performs phase two of the translation process.
	 */
	protected static class CFGTranslationPhaseTwo {

		/**
		 * Perform phase two of the translation.
		 * 
		 * @param in
		 *            The result of phase one.
		 * @return A control flow graph that might still contain empty basic
		 *         blocks.
		 */
		public static ControlFlowGraph process(PhaseOneResult in) {

			Map<Label, Integer> bindings = in.bindings;
			ArrayList<ExtendedNode> nodeList = in.nodeList;
			Set<Integer> leaders = in.leaders;

			assert in.nodeList.size() > 0;

			// exit blocks
			SpecialBlockImpl regularExitBlock = new SpecialBlockImpl(
					SpecialBlockType.EXIT);
			SpecialBlockImpl exceptionalExitBlock = new SpecialBlockImpl(
					SpecialBlockType.EXCEPTIONAL_EXIT);

			// record missing edges that will be added later
			Set<Tuple<? extends SingleSuccessorBlockImpl, Integer, ?>> missingEdges = new HashSet<>();

			// missing exceptional edges (third type argument should really be
			// Class<? extends Throwable>, but Java complains.
			Set<Tuple<ExceptionBlockImpl, Integer, ?>> missingExceptionalEdges = new HashSet<>();

			// create start block
			SpecialBlockImpl startBlock = new SpecialBlockImpl(
					SpecialBlockType.ENTRY);
			missingEdges.add(new Tuple<>(startBlock, 0));

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
				case CONDITIONAL_JUMP:
					// no label is supposed to point to conditional marker
					// nodes, thus we do not need to set block for 'node'
					assert block != null;
					final ConditionalBlockImpl cb = new ConditionalBlockImpl();
					block.setSuccessor(cb);
					block = new RegularBlockImpl();
					// use two anonymous SingleSuccessorBlockImpl that set the
					// 'then' and 'else' successor of the conditional block
					missingEdges.add(new Tuple<>(
							new SingleSuccessorBlockImpl() {
								@Override
								public void setSuccessor(BlockImpl successor) {
									cb.setThenSuccessor(successor);
								}
							}, i + 1));
					missingEdges.add(new Tuple<>(
							new SingleSuccessorBlockImpl() {
								@Override
								public void setSuccessor(BlockImpl successor) {
									cb.setElseSuccessor(successor);
								}
							}, bindings.get(node.getLabel())));
					break;
				case UNCONDITIONAL_JUMP:
					node.setBlock(block);
					if (node.getLabel() == regularExitLabel) {
						block.setSuccessor(regularExitBlock);
					} else if (node.getLabel() == exceptionalExitLabel) {
						block.setSuccessor(exceptionalExitBlock);
					} else {
						missingEdges.add(new Tuple<>(block, bindings.get(node
								.getLabel())));
					}
					block = new RegularBlockImpl();
					break;
				case EXCEPTION_NODE:
					NodeWithExceptionsHolder en = (NodeWithExceptionsHolder) node;
					// create new exception block and link with previous block
					ExceptionBlockImpl e = new ExceptionBlockImpl();
					e.setNode(en.getNode());
					block.setSuccessor(e);
					block = new RegularBlockImpl();

					// ensure linking between e and next block (normal edge)
					missingEdges.add(new Tuple<>(e, i + 1));

					// exceptional edges
					for (Entry<Class<? extends Throwable>, Label> entry : en
							.getExceptions().entrySet()) {
						// missingEdges.put(e, bindings.get(key))
						Integer target = bindings.get(entry.getValue());
						Class<? extends Throwable> cause = entry.getKey();
						missingExceptionalEdges.add(new Tuple<>(e, target,
								cause));
					}
					break;
				}
				i++;
			}

			// add missing edges
			for (Tuple<? extends SingleSuccessorBlockImpl, Integer, ?> p : missingEdges) {
				Integer index = p.b;
				ExtendedNode extendedNode = nodeList.get(index);
				BlockImpl target = extendedNode.getBlock();
				SingleSuccessorBlockImpl source = p.a;
				source.setSuccessor(target);
			}

			// add missing exceptional edges
			for (Tuple<ExceptionBlockImpl, Integer, ?> p : missingExceptionalEdges) {
				Integer index = p.b;
				@SuppressWarnings("unchecked")
				Class<? extends Throwable> cause = (Class<? extends Throwable>) p.c;
				ExceptionBlockImpl source = p.a;
				if (index == null) {
					// edge to exceptional exit
					source.addExceptionalSuccessor(exceptionalExitBlock, cause);
				} else {
					// edge to specific target
					ExtendedNode extendedNode = nodeList.get(index);
					BlockImpl target = extendedNode.getBlock();
					source.addExceptionalSuccessor(target, cause);
				}
			}

			return new ControlFlowGraph(startBlock, in.tree, in.treeLookupMap);
		}
	}

	/* --------------------------------------------------------- */
	/* Phase One */
	/* --------------------------------------------------------- */

	/**
	 * A wrapper object to pass around the result of phase one.
	 */
	protected static class PhaseOneResult {

		private IdentityHashMap<Tree, Node> treeLookupMap;
		private MethodTree tree;
		private Map<Label, Integer> bindings;
		private ArrayList<ExtendedNode> nodeList;
		private Set<Integer> leaders;

		public PhaseOneResult(MethodTree t,
				IdentityHashMap<Tree, Node> treeLookupMap,
				ArrayList<ExtendedNode> nodeList, Map<Label, Integer> bindings,
				Set<Integer> leaders) {
			this.tree = t;
			this.treeLookupMap = treeLookupMap;
			this.nodeList = nodeList;
			this.bindings = bindings;
			this.leaders = leaders;
		}

	}

	/**
	 * Class that performs phase one of the translation process. It generates
	 * the following information:
	 * <ul>
	 * <li>A sequence of extended nodes.</li>
	 * <li>A set of bindings from {@link Label}s to positions in the node
	 * sequence.</li>
	 * <li>A set of leader nodes that give rise to basic blocks in phase two.</li>
	 * <li>A lookup map that gives the mapping from AST tree nodes to
	 * {@link Node}s.</li>
	 * </ul>
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
	protected static class CFGTranslationPhaseOne implements
			TreeVisitor<Node, Void> {

		/**
		 * The translation starts in regular mode, that is
		 * <code>conditionalMode</code> is false. In this case, no conditional
		 * jump nodes are generated.
		 * 
		 * To correctly model control flow when the evaluation of an expression
		 * determines control flow (e.g. for if-conditions, while loops, or
		 * short-circuiting conditional expressions),
		 * <code>conditionalMode</code> can be set to true.
		 */
		protected boolean conditionalMode;
		
		protected Label trueTarget;
		protected Label falseTarget;

		/** Map from AST {@link Tree}s to {@link Node}s. */
		// TODO: fill this map with contents.
		protected IdentityHashMap<Tree, Node> treeLookupMap;

		/** The list of extended nodes. */
		protected ArrayList<ExtendedNode> nodeList;

		/** The bindings. */
		protected Map<Label, Integer> bindings;

		/** The set of leaders. */
		protected Set<Integer> leaders;

		/**
		 * Performs the actual work of phase one.
		 * 
		 * @param t
		 *            A method (identified by its AST element).
		 * @return The result of phase one.
		 */
		public PhaseOneResult process(MethodTree t) {

			// start in regular mode
			conditionalMode = false;

			// initialize lists and maps
			treeLookupMap = new IdentityHashMap<>();
			nodeList = new ArrayList<>();
			bindings = new HashMap<>();
			leaders = new HashSet<>();

			// traverse AST of the method body
			t.getBody().accept(this, null);

			// add marker to indicate that the next block will be the exit block
			nodeList.add(new UnconditionalJump(regularExitLabel));

			return new PhaseOneResult(t, treeLookupMap, nodeList, bindings,
					leaders);
		}

		/* --------------------------------------------------------- */
		/* Nodes and Labels Management */
		/* --------------------------------------------------------- */

		/**
		 * Extend the list of extended nodes with a node.
		 * 
		 * @param node
		 *            The node to add.
		 * @return The same node (for convenience).
		 */
		protected Node extendWithNode(Node node) {
			extendWithExtendedNode(new NodeHolder(node));
			return node;
		}

		/**
		 * Extend the list of extended nodes with a node, where
		 * <code>node</code> might throw the exception <code>cause</code>.
		 * 
		 * @param node
		 *            The node to add.
		 * @param causes
		 *            Set of exceptions that the node might throw.
		 * @return The same node (for convenience).
		 */
		protected Node extendWithNodeWithException(Node node,
				Class<? extends Throwable> cause) {
			Set<Class<? extends Throwable>> causes = new HashSet<>();
			causes.add(cause);
			return extendWithNodeWithExceptions(node, causes);
		}

		/**
		 * Extend the list of extended nodes with a node, where
		 * <code>node</code> might throw any of the exception in
		 * <code>causes</code>.
		 * 
		 * @param node
		 *            The node to add.
		 * @param causes
		 *            Set of exceptions that the node might throw.
		 * @return The same node (for convenience).
		 */
		protected Node extendWithNodeWithExceptions(Node node,
				Set<Class<? extends Throwable>> causes) {
			// TODO: catch blocks
			Map<Class<? extends Throwable>, Label> exceptions = new HashMap<>();
			for (Class<? extends Throwable> cause : causes) {
				exceptions.put(cause, exceptionalExitLabel);
			}
			NodeWithExceptionsHolder exNode = new NodeWithExceptionsHolder(
					node, exceptions);
			extendWithExtendedNode(exNode);
			return node;
		}

		/**
		 * Extend the list of extended nodes with an extended node.
		 * 
		 * @param n
		 *            The extended node.
		 */
		protected void extendWithExtendedNode(ExtendedNode n) {
			nodeList.add(n);
		}

		/**
		 * Add the label {@code l} to the extended node that will be placed next
		 * in the sequence.
		 */
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
					extendWithNodeWithException(target,
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
				
				Label oldTrueTarget = trueTarget;
				Label oldFalseTarget = falseTarget;
				Label myTrueTarget = new Label();
				trueTarget = myTrueTarget;
				Label myFalseTarget = new Label();
				falseTarget = myFalseTarget;

				// left-hand side
				Label leftFalse = new Label();
				Node left = tree.getLeftOperand().accept(this, p);

				// right-hand side
				Label rightFalse = new Label();
				Node right = tree.getRightOperand().accept(this, p);

				conditionalMode = condMode;

				if (conditionalMode) {
					// node for true case
					Node trueNode = new ConditionalOrNode(tree, left, right,
							true);

					// node for false case
					Node falseNode = new ConditionalOrNode(tree, left, right,
							false);

					return trueNode;
				} else {
					// one node for true/false
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

			// basic block for the condition
			conditionalMode = true;
			tree.getCondition().accept(this, null);
			conditionalMode = false;
			Label elseEntry = new Label();
			extendWithExtendedNode(new ConditionalJump(elseEntry));

			// then branch
			Label endIf = new Label();
			StatementTree thenStatement = tree.getThenStatement();
			thenStatement.accept(this, null);
			extendWithExtendedNode(new UnconditionalJump(endIf));

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
