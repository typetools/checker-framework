package org.checkerframework.dataflow.cfg.visualize;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Analysis.Direction;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;
import org.plumelib.util.StringsPlume;
import org.plumelib.util.UniqueId;

/**
 * This abstract class makes implementing a {@link CFGVisualizer} easier. Some of the methods in
 * {@link CFGVisualizer} are already implemented in this abstract class, but can be overridden if
 * necessary.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximate runtime behavior
 * @see DOTCFGVisualizer
 * @see StringCFGVisualizer
 */
public abstract class AbstractCFGVisualizer<
        V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
    implements CFGVisualizer<V, S, T> {

  /**
   * If {@code true}, {@link CFGVisualizer} returns more detailed information.
   *
   * <p>Initialized in {@link #init(Map)}.
   */
  protected boolean verbose;

  /** The line separator. */
  protected final String lineSeparator = System.lineSeparator();

  /** The indentation for elements of the store. */
  protected final String storeEntryIndent = "  ";

  @Override
  public void init(Map<String, Object> args) {
    this.verbose = toBoolean(args.get("verbose"));
  }

  /**
   * Convert the value to boolean, by parsing a string or casting any other value. null converts to
   * false.
   *
   * @param o an object to convert to boolean
   * @return {@code o} converted to boolean
   */
  private static boolean toBoolean(@Nullable Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof String) {
      return Boolean.parseBoolean((String) o);
    }
    return (boolean) o;
  }

  /**
   * Visualize a control flow graph.
   *
   * @param cfg the current control flow graph
   * @param entry the entry block of the control flow graph
   * @param analysis the current analysis
   * @return the representation of the control flow graph
   */
  protected String visualizeGraph(
      ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {
    return visualizeGraphHeader()
        + visualizeGraphWithoutHeaderAndFooter(cfg, entry, analysis)
        + visualizeGraphFooter();
  }

  /**
   * Helper method to visualize a control flow graph, without outputting a header or footer.
   *
   * @param cfg the control flow graph
   * @param entry the entry block of the control flow graph
   * @param analysis the current analysis
   * @return the String representation of the control flow graph
   */
  protected String visualizeGraphWithoutHeaderAndFooter(
      ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {
    Set<Block> visited = new LinkedHashSet<>();
    StringBuilder sbGraph = new StringBuilder();
    Queue<Block> workList = new ArrayDeque<>();
    Block cur = entry;
    visited.add(entry);
    while (cur != null) {
      handleSuccessorsHelper(cur, visited, workList, sbGraph);
      cur = workList.poll();
    }
    sbGraph.append(lineSeparator);
    sbGraph.append(visualizeNodes(visited, cfg, analysis));
    return sbGraph.toString();
  }

  /**
   * Outputs, to sbGraph, a visualization of a block's edges, but not the block itself. (The block
   * itself is output elsewhere.) Also adds the successors of the block to the work list and the
   * visited blocks list.
   *
   * @param cur the current block
   * @param visited the set of blocks that have already been visited or are in the work list; side
   *     effected by this method
   * @param workList the queue of blocks to be processed; side effected by this method
   * @param sbGraph the {@link StringBuilder} to store the graph; side effected by this method
   */
  protected void handleSuccessorsHelper(
      Block cur, Set<Block> visited, Queue<Block> workList, StringBuilder sbGraph) {
    if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
      ConditionalBlock ccur = ((ConditionalBlock) cur);
      Block thenSuccessor = ccur.getThenSuccessor();
      sbGraph.append(
          visualizeEdge(ccur.getUid(), thenSuccessor.getUid(), ccur.getThenFlowRule().toString()));
      sbGraph.append(lineSeparator);
      addBlock(thenSuccessor, visited, workList);
      Block elseSuccessor = ccur.getElseSuccessor();
      sbGraph.append(
          visualizeEdge(ccur.getUid(), elseSuccessor.getUid(), ccur.getElseFlowRule().toString()));
      sbGraph.append(lineSeparator);
      addBlock(elseSuccessor, visited, workList);
    } else {
      SingleSuccessorBlock sscur = (SingleSuccessorBlock) cur;
      Block succ = sscur.getSuccessor();
      if (succ != null) {
        sbGraph.append(visualizeEdge(cur.getUid(), succ.getUid(), sscur.getFlowRule().name()));
        sbGraph.append(lineSeparator);
        addBlock(succ, visited, workList);
      }
    }
    if (cur.getType() == Block.BlockType.EXCEPTION_BLOCK) {
      ExceptionBlock ecur = (ExceptionBlock) cur;
      for (Map.Entry<TypeMirror, Set<Block>> e : ecur.getExceptionalSuccessors().entrySet()) {
        TypeMirror cause = e.getKey();
        String exception = cause.toString();
        if (exception.startsWith("java.lang.")) {
          exception = exception.replace("java.lang.", "");
        }
        for (Block b : e.getValue()) {
          sbGraph.append(visualizeEdge(cur.getUid(), b.getUid(), exception));
          sbGraph.append(lineSeparator);
          addBlock(b, visited, workList);
        }
      }
    }
  }

  /**
   * Checks whether a block exists in the visited blocks list, and, if not, adds it to the visited
   * blocks list and the work list.
   *
   * @param b the block to check
   * @param visited the set of blocks that have already been visited or are in the work list
   * @param workList the queue of blocks to be processed
   */
  protected void addBlock(Block b, Set<Block> visited, Queue<Block> workList) {
    if (visited.add(b)) {
      workList.add(b);
    }
  }

  /**
   * Helper method to visualize a block.
   *
   * <p>NOTE: The output ends with a separator, only if an "after" store is visualized. The client
   * {@link #visualizeBlock} should correct this if needed.
   *
   * @param bb the block
   * @param analysis the current analysis
   * @param separator the line separator. Examples: "\\l" for left justification in {@link
   *     DOTCFGVisualizer} (this is really a terminator, not a separator), "\n" to add a new line in
   *     {@link StringCFGVisualizer}
   * @return the String representation of the block
   */
  protected String visualizeBlockHelper(
      Block bb, @Nullable Analysis<V, S, T> analysis, String separator) {
    StringBuilder sbBlock = new StringBuilder();
    String contents = loopOverBlockContents(bb, analysis, separator);
    if (!contents.isEmpty()) {
      sbBlock.append(contents);
    }
    if (sbBlock.length() == 0) {
      // Nothing got appended; use default text for empty block
      if (bb.getType() == Block.BlockType.SPECIAL_BLOCK) {
        sbBlock.append(visualizeSpecialBlock((SpecialBlock) bb));
      } else if (bb.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
        sbBlock.append(visualizeConditionalBlock((ConditionalBlock) bb));
      } else {
        sbBlock.append("<empty block>");
      }
    }

    // Visualize transfer input if necessary.
    if (analysis != null) {
      sbBlock.insert(0, visualizeBlockTransferInputBefore(bb, analysis) + separator);
      if (verbose) {
        Node lastNode = bb.getLastNode();
        if (lastNode != null) {
          if (!sbBlock.toString().endsWith(separator)) {
            sbBlock.append(separator);
          }
          sbBlock.append(visualizeBlockTransferInputAfter(bb, analysis) + separator);
        }
      }
    }
    return sbBlock.toString();
  }

  /**
   * Iterates over the block content and visualizes all the nodes in it.
   *
   * @param bb the block
   * @param analysis the current analysis
   * @param separator the separator between the nodes of the block
   * @return the String representation of the contents of the block
   */
  protected String loopOverBlockContents(
      Block bb, @Nullable Analysis<V, S, T> analysis, String separator) {

    List<Node> contents = addBlockContent(bb);
    StringJoiner sjBlockContents = new StringJoiner(separator);
    for (Node t : contents) {
      sjBlockContents.add(visualizeBlockNode(t, analysis));
    }
    return sjBlockContents.toString();
  }

  /**
   * Returns the contents of the block.
   *
   * @param bb the block
   * @return the contents of the block, as a list of nodes
   */
  protected List<Node> addBlockContent(Block bb) {
    return bb.getNodes();
  }

  /**
   * Format the given object as a String suitable for the output format, i.e. with format-specific
   * characters escaped.
   *
   * @param obj an object
   * @return the formatted String from the given object
   */
  protected abstract String format(Object obj);

  @Override
  public String visualizeBlockNode(Node t, @Nullable Analysis<V, S, T> analysis) {
    StringBuilder sbBlockNode = new StringBuilder();
    sbBlockNode.append(format(t)).append("   [ ").append(getNodeSimpleName(t)).append(" ]");
    if (analysis != null) {
      V value = analysis.getValue(t);
      if (value != null) {
        sbBlockNode.append("    > ").append(format(value));
      }
    }
    return sbBlockNode.toString();
  }

  /** Whether to visualize before or after a block. */
  protected enum VisualizeWhere {
    /** Visualize before the block. */
    BEFORE,
    /** Visualize after the block. */
    AFTER
  }

  /**
   * Visualize the transfer input before or after the given block.
   *
   * @param where either BEFORE or AFTER
   * @param bb a block
   * @param analysis the current analysis
   * @param separator the line separator. Examples: "\\l" for left justification in {@link
   *     DOTCFGVisualizer} (which is actually a line TERMINATOR, not a separator!), "\n" to add a
   *     new line in {@link StringCFGVisualizer}
   * @return the visualization of the transfer input before or after the given block
   */
  protected String visualizeBlockTransferInputHelper(
      VisualizeWhere where, Block bb, Analysis<V, S, T> analysis, String separator) {
    if (analysis == null) {
      throw new BugInCF(
          "analysis must be non-null when visualizing the transfer input of a block.");
    }

    Direction analysisDirection = analysis.getDirection();

    S regularStore;
    S thenStore = null;
    S elseStore = null;
    boolean isTwoStores = false;

    UniqueId storesFrom;

    if (analysisDirection == Direction.FORWARD && where == VisualizeWhere.AFTER) {
      regularStore = analysis.getResult().getStoreAfter(bb);
      storesFrom = analysis.getResult();
    } else if (analysisDirection == Direction.BACKWARD && where == VisualizeWhere.BEFORE) {
      regularStore = analysis.getResult().getStoreBefore(bb);
      storesFrom = analysis.getResult();
    } else {
      TransferInput<V, S> input = analysis.getInput(bb);
      assert input != null : "@AssumeAssertion(nullness): invariant";
      storesFrom = input;
      isTwoStores = input.containsTwoStores();
      regularStore = input.getRegularStore();
      thenStore = input.getThenStore();
      elseStore = input.getElseStore();
    }

    StringBuilder sbStore = new StringBuilder();
    if (verbose) {
      sbStore.append(storesFrom.getClassAndUid() + separator);
    }
    sbStore.append(where == VisualizeWhere.BEFORE ? "Before: " : "After: ");

    if (!isTwoStores) {
      sbStore.append(visualizeStore(regularStore));
    } else {
      assert thenStore != null : "@AssumeAssertion(nullness): invariant";
      assert elseStore != null : "@AssumeAssertion(nullness): invariant";
      sbStore.append("then=");
      sbStore.append(visualizeStore(thenStore));
      sbStore.append(",");
      sbStore.append(separator);
      sbStore.append("else=");
      sbStore.append(visualizeStore(elseStore));
    }
    if (where == VisualizeWhere.BEFORE) {
      sbStore.append(separator + "~~~~~~~~~");
    } else {
      sbStore.insert(0, "~~~~~~~~~" + separator);
    }
    return sbStore.toString();
  }

  /**
   * Visualize a special block.
   *
   * @param sbb the special block
   * @return the String representation of the special block
   */
  protected String visualizeSpecialBlockHelper(SpecialBlock sbb) {
    switch (sbb.getSpecialType()) {
      case ENTRY:
        return "<entry>";
      case EXIT:
        return "<exit>";
      case EXCEPTIONAL_EXIT:
        return "<exceptional-exit>";
      default:
        throw new BugInCF("Unrecognized special block type: " + sbb.getType());
    }
  }

  /**
   * Generate the order of processing blocks. Because a block may appears more than once in {@link
   * ControlFlowGraph#getDepthFirstOrderedBlocks()}, the orders of each block are stored in a
   * separate array list.
   *
   * @param cfg the current control flow graph
   * @return an IdentityHashMap that maps from blocks to their orders
   */
  protected IdentityHashMap<Block, List<Integer>> getProcessOrder(ControlFlowGraph cfg) {
    IdentityHashMap<Block, List<Integer>> depthFirstOrder = new IdentityHashMap<>();
    int count = 1;
    for (Block b : cfg.getDepthFirstOrderedBlocks()) {
      depthFirstOrder.computeIfAbsent(b, k -> new ArrayList<>());
      @SuppressWarnings("nullness:assignment") // computeIfAbsent's function doesn't return null
      @NonNull List<Integer> blockIds = depthFirstOrder.get(b);
      blockIds.add(count++);
    }
    return depthFirstOrder;
  }

  @Override
  public String visualizeStore(S store) {
    return store.visualize(this);
  }

  /**
   * Generate the String representation of the nodes of a control flow graph.
   *
   * @param blocks the set of all the blocks in a control flow graph
   * @param cfg the control flow graph
   * @param analysis the current analysis
   * @return the String representation of the nodes
   */
  protected abstract String visualizeNodes(
      Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<V, S, T> analysis);

  /**
   * Generate the String representation of an edge.
   *
   * @param sId a representation of the current block, such as its ID
   * @param eId a representation of the successor block, such as its ID
   * @param flowRule the content of the edge
   * @return the String representation of the edge
   */
  protected abstract String visualizeEdge(Object sId, Object eId, String flowRule);

  /**
   * Return the header of the generated graph.
   *
   * @return the String representation of the header of the control flow graph
   */
  protected abstract String visualizeGraphHeader();

  /**
   * Return the footer of the generated graph.
   *
   * @return the String representation of the footer of the control flow graph
   */
  protected abstract String visualizeGraphFooter();

  /**
   * Given a list of process orders (integers), returns a string representation.
   *
   * <p>Examples: "Process order: 23", "Process order: 23,25".
   *
   * @param order a list of process orders
   * @return a String representation of the given process orders
   */
  protected String getProcessOrderSimpleString(List<Integer> order) {
    return "Process order: " + StringsPlume.join(",", order);
  }

  /**
   * Get the simple name of a node.
   *
   * @param t a node
   * @return the node's simple name, without "Node"
   */
  protected String getNodeSimpleName(Node t) {
    String name = t.getClass().getSimpleName();
    return name.replace("Node", "");
  }
}
