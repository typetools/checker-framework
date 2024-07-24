package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;

/**
 * Implementation of common features for {@link BackwardAnalysisImpl} and {@link
 * ForwardAnalysisImpl}.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximate run-time behavior
 */
public abstract class AbstractAnalysis<
        V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
    implements Analysis<V, S, T> {

  /** The direction of this analysis. */
  protected final Direction direction;

  /** Is the analysis currently running? */
  protected boolean isRunning = false;

  /** The transfer function for regular nodes. */
  // TODO: make final. Currently, the transferFunction has a reference to the analysis, so it
  //  can't be created until the Analysis is initialized.
  protected @MonotonicNonNull T transferFunction;

  /** The current control flow graph to perform the analysis on. */
  protected @MonotonicNonNull ControlFlowGraph cfg;

  /**
   * The transfer inputs of every basic block; assumed to be 'no information' if not present. The
   * inputs are before blocks in forward analysis, and are after blocks in backward analysis.
   */
  protected final IdentityHashMap<Block, TransferInput<V, S>> inputs = new IdentityHashMap<>();

  /** The worklist used for the fix-point iteration. */
  protected final Worklist worklist;

  /** Abstract values of nodes. */
  protected final IdentityHashMap<Node, V> nodeValues = new IdentityHashMap<>();

  /** Map from (effectively final) local variable elements to their abstract value. */
  protected final HashMap<VariableElement, V> finalLocalValues = new HashMap<>();

  /**
   * The node that is currently handled in the analysis (if it is running). The following invariant
   * holds:
   *
   * <pre>
   *   !isRunning &rArr; (currentNode == null)
   * </pre>
   */
  // currentNode == null when isRunning is true.
  // See https://github.com/typetools/checker-framework/issues/4115
  protected @InternedDistinct @Nullable Node currentNode;

  /**
   * The tree that is currently being looked at. The transfer function can set this tree to make
   * sure that calls to {@code getValue} will not return information for this given tree.
   */
  protected @InternedDistinct @Nullable Tree currentTree;

  /** The current transfer input when the analysis is running. */
  protected @Nullable TransferInput<V, S> currentInput;

  /**
   * Returns the tree that is currently being looked at. The transfer function can set this tree to
   * make sure that calls to {@code getValue} will not return information for this given tree.
   *
   * @return the tree that is currently being looked at
   */
  public @Nullable Tree getCurrentTree() {
    return currentTree;
  }

  /**
   * Set the tree that is currently being looked at.
   *
   * @param currentTree the tree that should be currently looked at
   */
  public void setCurrentTree(@FindDistinct Tree currentTree) {
    this.currentTree = currentTree;
  }

  /**
   * Set the node that is currently being looked at.
   *
   * @param currentNode the node that should be currently looked at
   */
  protected void setCurrentNode(@FindDistinct @Nullable Node currentNode) {
    this.currentNode = currentNode;
  }

  /**
   * Implementation of common features for {@link BackwardAnalysisImpl} and {@link
   * ForwardAnalysisImpl}.
   *
   * @param direction direction of the analysis
   */
  protected AbstractAnalysis(Direction direction) {
    this.direction = direction;
    this.worklist = new Worklist(this.direction);
  }

  /** Initialize the transfer inputs of every basic block before performing the analysis. */
  @RequiresNonNull("cfg")
  protected abstract void initInitialInputs();

  /**
   * Propagate the stores in {@code currentInput} to the next block in the direction of analysis,
   * according to the {@code flowRule}.
   *
   * @param nextBlock the target block to propagate the stores to
   * @param node the node of the target block
   * @param currentInput the current transfer input
   * @param flowRule the flow rule being used
   * @param addToWorklistAgain whether the block should be added to {@link #worklist} again
   */
  protected abstract void propagateStoresTo(
      Block nextBlock,
      Node node,
      TransferInput<V, S> currentInput,
      Store.FlowRule flowRule,
      boolean addToWorklistAgain);

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  @Override
  public Direction getDirection() {
    return this.direction;
  }

  /** A cache for {@link #getResult()}. */
  private @Nullable AnalysisResult<V, S> getResultCache;

  @Override
  @SuppressWarnings("nullness:contracts.precondition.override") // implementation field
  @RequiresNonNull("cfg")
  public AnalysisResult<V, S> getResult() {
    if (isRunning) {
      throw new BugInCF(
          "AbstractAnalysis::getResult() shouldn't be called when the analysis is running.");
    }
    if (getResultCache == null) {
      getResultCache =
          new AnalysisResult<>(
              nodeValues,
              inputs,
              cfg.getTreeLookup(),
              cfg.getPostfixNodeLookup(),
              finalLocalValues);
    }
    return getResultCache;
  }

  @Override
  public @Nullable T getTransferFunction() {
    return transferFunction;
  }

  @Override
  public @Nullable V getValue(Node n) {
    if (isRunning) {
      // we don't have a org.checkerframework.dataflow fact about the current node yet
      if (currentNode == null
          || currentNode == n
          || (currentTree != null && currentTree == n.getTree())) {
        return null;
      }
      // check that 'n' is a subnode of 'currentNode'. Check immediate operands
      // first for efficiency.
      assert !n.isLValue() : "Did not expect an lvalue, but got " + n;
      if (!currentNode.getOperands().contains(n)
          && !currentNode.getTransitiveOperands().contains(n)) {
        return null;
      }
      // fall through when the current node is not 'n', and 'n' is not a subnode.
    }
    return nodeValues.get(n);
  }

  /**
   * Returns all current node values.
   *
   * @return {@link #nodeValues}
   */
  public IdentityHashMap<Node, V> getNodeValues() {
    return nodeValues;
  }

  /**
   * Set all current node values to the given map.
   *
   * @param in the current node values
   */
  /*package-private*/ void setNodeValues(IdentityHashMap<Node, V> in) {
    assert !isRunning;
    nodeValues.clear();
    nodeValues.putAll(in);
  }

  @Override
  @SuppressWarnings("nullness:contracts.precondition.override") // implementation field
  @RequiresNonNull("cfg")
  public @Nullable S getRegularExitStore() {
    SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
    if (inputs.containsKey(regularExitBlock)) {
      return inputs.get(regularExitBlock).getRegularStore();
    } else {
      return null;
    }
  }

  @Override
  @SuppressWarnings("nullness:contracts.precondition.override") // implementation field
  @RequiresNonNull("cfg")
  public @Nullable S getExceptionalExitStore() {
    SpecialBlock exceptionalExitBlock = cfg.getExceptionalExitBlock();
    if (inputs.containsKey(exceptionalExitBlock)) {
      S exceptionalExitStore = inputs.get(exceptionalExitBlock).getRegularStore();
      return exceptionalExitStore;
    } else {
      return null;
    }
  }

  /**
   * Get the set of {@link Node}s for a given {@link Tree}. Returns null for trees that don't
   * produce a value.
   *
   * @param t the given tree
   * @return the set of corresponding nodes to the given tree
   */
  public @Nullable Set<Node> getNodesForTree(Tree t) {
    if (cfg == null) {
      return null;
    }
    return cfg.getNodesCorrespondingToTree(t);
  }

  @Override
  public @Nullable V getValue(Tree t) {
    // Dataflow is analyzing the tree, so no value is available.
    if (t == currentTree || cfg == null) {
      return null;
    }
    V result = getValue(getNodesForTree(t));
    if (result == null) {
      result = getValue(cfg.getTreeLookup().get(t));
    }
    return result;
  }

  /**
   * Returns the least upper bound of the values of {@code nodes}.
   *
   * @param nodes a set of nodes
   * @return the least upper bound of the values of {@code nodes}
   */
  private @Nullable V getValue(@Nullable Set<Node> nodes) {
    if (nodes == null) {
      return null;
    }

    V merged = null;
    for (Node aNode : nodes) {
      if (aNode.isLValue()) {
        return null;
      }
      V v = getValue(aNode);
      if (merged == null) {
        merged = v;
      } else if (v != null) {
        merged = merged.leastUpperBound(v);
      }
    }

    return merged;
  }

  /**
   * Get the {@link MethodTree} of the current CFG if the argument {@link Tree} maps to a {@link
   * Node} in the CFG or {@code null} otherwise.
   *
   * @param t the given tree
   * @return the contained method tree of the given tree
   * @deprecated use {@link #getEnclosingMethod}
   */
  @Deprecated // 2024-05-01
  public @Nullable MethodTree getContainingMethod(Tree t) {
    return getEnclosingMethod(t);
  }

  /**
   * Get the {@link MethodTree} of the current CFG if the argument {@link Tree} maps to a {@link
   * Node} in the CFG or {@code null} otherwise.
   *
   * @param t the given tree
   * @return the contained method tree of the given tree
   */
  public @Nullable MethodTree getEnclosingMethod(Tree t) {
    if (cfg == null) {
      return null;
    }
    return cfg.getEnclosingMethod(t);
  }

  /**
   * Get the {@link ClassTree} of the current CFG if the argument {@link Tree} maps to a {@link
   * Node} in the CFG or {@code null} otherwise.
   *
   * @param t the given tree
   * @return the contained class tree of the given tree
   * @deprecated use {@link #getEnclosingClass}
   */
  @Deprecated // 2024-05-01
  public @Nullable ClassTree getContainingClass(Tree t) {
    return getEnclosingClass(t);
  }

  /**
   * Get the {@link ClassTree} of the current CFG if the argument {@link Tree} maps to a {@link
   * Node} in the CFG or {@code null} otherwise.
   *
   * @param t the given tree
   * @return the contained class tree of the given tree
   */
  public @Nullable ClassTree getEnclosingClass(Tree t) {
    if (cfg == null) {
      return null;
    }
    return cfg.getEnclosingClass(t);
  }

  /**
   * Call the transfer function for node {@code node}, and set that node as current node first. This
   * method requires a {@code transferInput} that the method can modify.
   *
   * @param node the given node
   * @param transferInput the transfer input
   * @return the output of the transfer function
   */
  protected TransferResult<V, S> callTransferFunction(
      Node node, TransferInput<V, S> transferInput) {
    assert transferFunction != null : "@AssumeAssertion(nullness): invariant";
    if (node.isLValue()) {
      // TODO: should the default behavior return a regular transfer result, a conditional
      // transfer result (depending on store.containsTwoStores()), or is the following
      // correct?
      return new RegularTransferResult<>(null, transferInput.getRegularStore());
    }
    transferInput.node = node;
    setCurrentNode(node);
    @SuppressWarnings("nullness") // CF bug: "INFERENCE FAILED"
    TransferResult<V, S> transferResult = node.accept(transferFunction, transferInput);
    setCurrentNode(null);
    if (node instanceof AssignmentNode) {
      // store the flow-refined value effectively for final local variables
      AssignmentNode assignment = (AssignmentNode) node;
      Node lhst = assignment.getTarget();
      if (lhst instanceof LocalVariableNode) {
        LocalVariableNode lhs = (LocalVariableNode) lhst;
        VariableElement elem = lhs.getElement();
        if (ElementUtils.isEffectivelyFinal(elem)) {
          V resval = transferResult.getResultValue();
          if (resval != null) {
            finalLocalValues.put(elem, resval);
          }
        }
      }
    }
    return transferResult;
  }

  /**
   * Initialize the analysis with a new control flow graph.
   *
   * @param cfg the control flow graph to use
   */
  protected final void init(ControlFlowGraph cfg) {
    initFields(cfg);
    initInitialInputs();
  }

  /**
   * Should exceptional control flow for a particular exception type be ignored?
   *
   * <p>The default implementation always returns {@code false}. Subclasses should override the
   * method to implement a different policy.
   *
   * @param exceptionType the exception type
   * @return {@code true} if exceptional control flow due to {@code exceptionType} should be
   *     ignored, {@code false} otherwise
   */
  protected boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return false;
  }

  /**
   * Initialize fields of this object based on a given control flow graph. Sub-class may override
   * this method to initialize customized fields.
   *
   * @param cfg a given control flow graph
   */
  @EnsuresNonNull("this.cfg")
  protected void initFields(ControlFlowGraph cfg) {
    inputs.clear();
    nodeValues.clear();
    finalLocalValues.clear();
    this.cfg = cfg;
    getResultCache = null;
  }

  /**
   * Updates the value of node {@code node} in {@link #nodeValues} to the value of the {@code
   * transferResult}. Returns true if the node's value changed, or a store was updated.
   *
   * @param node the node to update
   * @param transferResult the transfer result being updated
   * @return true if the node's value changed, or a store was updated
   */
  protected boolean updateNodeValues(Node node, TransferResult<V, S> transferResult) {
    V newVal = transferResult.getResultValue();
    boolean nodeValueChanged = false;
    if (newVal != null) {
      V oldVal = nodeValues.get(node);
      nodeValues.put(node, newVal);
      nodeValueChanged = !Objects.equals(oldVal, newVal);
    }
    return nodeValueChanged || transferResult.storeChanged();
  }

  /**
   * Add a basic block to {@link #worklist}. If {@code b} is already present, the method does
   * nothing.
   *
   * @param b the block to add to {@link #worklist}
   */
  protected void addToWorklist(Block b) {
    // TODO: This costs linear (!) time.  Use a more efficient way to check if b is already
    // present.
    // Two possibilities:
    //  * add unconditionally, and detect duplicates when removing from the queue.
    //  * maintain a HashSet of the elements that are already in the queue.
    if (!worklist.contains(b)) {
      worklist.add(b);
    }
  }

  /**
   * A worklist is a priority queue of blocks in which the order is given by depth-first ordering to
   * place non-loop predecessors ahead of successors.
   */
  protected static class Worklist {

    /** Map all blocks in the CFG to their depth-first order. */
    protected final IdentityHashMap<Block, Integer> depthFirstOrder = new IdentityHashMap<>();

    /**
     * Comparators to allow priority queue to order blocks by their depth-first order, using by
     * forward analysis.
     */
    public class ForwardDfoComparator implements Comparator<Block> {
      /** Creates a new ForwardDfoComparator. */
      public ForwardDfoComparator() {}

      @SuppressWarnings("nullness:unboxing.of.nullable")
      @Override
      public int compare(Block b1, Block b2) {
        return depthFirstOrder.get(b1) - depthFirstOrder.get(b2);
      }
    }

    /**
     * Comparators to allow priority queue to order blocks by their depth-first order, using by
     * backward analysis.
     */
    public class BackwardDfoComparator implements Comparator<Block> {
      /** Creates a new BackwardDfoComparator. */
      public BackwardDfoComparator() {}

      @SuppressWarnings("nullness:unboxing.of.nullable")
      @Override
      public int compare(Block b1, Block b2) {
        return depthFirstOrder.get(b2) - depthFirstOrder.get(b1);
      }
    }

    /** The backing priority queue. */
    protected final PriorityQueue<Block> queue;

    /** Contains the same elements as {@link #queue}, for faster lookup. */
    protected final Set<Block> queueSet;

    /**
     * Create a Worklist.
     *
     * @param direction the direction (forward or backward)
     */
    public Worklist(Direction direction) {
      if (direction == Direction.FORWARD) {
        queue = new PriorityQueue<>(new ForwardDfoComparator());
        queueSet = new HashSet<>();
      } else if (direction == Direction.BACKWARD) {
        queue = new PriorityQueue<>(new BackwardDfoComparator());
        queueSet = new HashSet<>();
      } else {
        throw new BugInCF("Unexpected Direction: " + direction.name());
      }
    }

    /**
     * Process the control flow graph.
     *
     * <p>This implementation sets the depth-first order for each block, by adding the blocks to
     * {@link #depthFirstOrder}.
     *
     * @param cfg the control flow graph to process
     */
    public void process(ControlFlowGraph cfg) {
      depthFirstOrder.clear();
      int count = 1;
      for (Block b : cfg.getDepthFirstOrderedBlocks()) {
        depthFirstOrder.put(b, count++);
      }

      queue.clear();
      queueSet.clear();
    }

    /**
     * See {@link PriorityQueue#isEmpty}.
     *
     * @see PriorityQueue#isEmpty
     * @return true if {@link #queue} is empty else false
     */
    @Pure
    @EnsuresNonNullIf(result = false, expression = "poll()")
    @SuppressWarnings("nullness:contracts.conditional.postcondition") // forwarded
    public boolean isEmpty() {
      assert queue.isEmpty() == queueSet.isEmpty();
      return queue.isEmpty();
    }

    /**
     * Check if {@link #queue} contains the block which is passed as the argument.
     *
     * @param block the given block to check
     * @return true if {@link #queue} contains the given block
     */
    public boolean contains(Block block) {
      return queueSet.contains(block);
    }

    /**
     * Add the given block to {@link #queue}. Adds unconditionally: does not check containment
     * first.
     *
     * @param block the block to add to {@link #queue}
     */
    public void add(Block block) {
      queue.add(block);
      queueSet.add(block);
    }

    /**
     * See {@link PriorityQueue#poll}.
     *
     * @see PriorityQueue#poll
     * @return the head of {@link #queue}
     */
    @Pure
    public @Nullable Block poll() {
      Block result = queue.poll();
      if (result != null) {
        queueSet.remove(result);
      }
      return result;
    }

    @Override
    public String toString() {
      return "Worklist(" + queue + ")";
    }
  }
}
