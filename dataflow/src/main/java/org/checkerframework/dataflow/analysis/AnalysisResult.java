package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.UniqueId;
import org.plumelib.util.UnmodifiableIdentityHashMap;

/**
 * An {@link AnalysisResult} represents the result of a org.checkerframework.dataflow analysis by
 * providing the abstract values given a node or a tree. Note that it does not keep track of custom
 * results computed by some analysis.
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public class AnalysisResult<V extends AbstractValue<V>, S extends Store<S>> implements UniqueId {

  /**
   * For efficiency, certain maps stored in the result are only copied lazily, when they need to be
   * mutated. This flag tracks if the copying has occurred.
   */
  private boolean mapsCopied = false;

  /** Abstract values of nodes. */
  protected IdentityHashMap<Node, V> nodeValues;

  /**
   * Map from AST {@link Tree}s to sets of {@link Node}s.
   *
   * <p>Some of those Nodes might not be keys in {@link #nodeValues}. One reason is that the Node is
   * unreachable in the control flow graph, so dataflow never gave it a value.
   */
  protected IdentityHashMap<Tree, Set<Node>> treeLookup;

  /**
   * Map from postfix increment or decrement trees that are AST {@link UnaryTree}s to the synthetic
   * tree that is {@code v + 1} or {@code v - 1}.
   */
  protected IdentityHashMap<UnaryTree, BinaryTree> postfixLookup;

  /** Map from (effectively final) local variable elements to their abstract value. */
  protected final HashMap<VariableElement, V> finalLocalValues;

  /** The stores before every method call. */
  protected final IdentityHashMap<Block, TransferInput<V, S>> stores;

  /**
   * Caches of the analysis results for each input for the block of the node and each node.
   *
   * @see #runAnalysisFor(Node, Analysis.BeforeOrAfter, TransferInput, IdentityHashMap, Map)
   */
  protected final Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>>
      analysisCaches;

  /** The unique ID for the next-created object. */
  static final AtomicLong nextUid = new AtomicLong(0);
  /** The unique ID of this object. */
  final transient long uid = nextUid.getAndIncrement();

  @Override
  public long getUid(@UnknownInitialization AnalysisResult<V, S> this) {
    return uid;
  }

  /**
   * Initialize with given mappings.
   *
   * @param nodeValues {@link #nodeValues}
   * @param stores {@link #stores}
   * @param treeLookup {@link #treeLookup}
   * @param postfixLookup {@link #postfixLookup}
   * @param finalLocalValues {@link #finalLocalValues}
   * @param analysisCaches {@link #analysisCaches}
   */
  protected AnalysisResult(
      IdentityHashMap<Node, V> nodeValues,
      IdentityHashMap<Block, TransferInput<V, S>> stores,
      IdentityHashMap<Tree, Set<Node>> treeLookup,
      IdentityHashMap<UnaryTree, BinaryTree> postfixLookup,
      HashMap<VariableElement, V> finalLocalValues,
      Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
    this.nodeValues = UnmodifiableIdentityHashMap.wrap(nodeValues);
    this.treeLookup = UnmodifiableIdentityHashMap.wrap(treeLookup);
    this.postfixLookup = UnmodifiableIdentityHashMap.wrap(postfixLookup);
    // TODO: why are stores and finalLocalValues captured?
    this.stores = stores;
    this.finalLocalValues = finalLocalValues;
    this.analysisCaches = analysisCaches;
  }

  /**
   * Initialize with given mappings and empty cache.
   *
   * @param nodeValues {@link #nodeValues}
   * @param stores {@link #stores}
   * @param treeLookup {@link #treeLookup}
   * @param postfixLookup {@link #postfixLookup}
   * @param finalLocalValues {@link #finalLocalValues}
   */
  public AnalysisResult(
      IdentityHashMap<Node, V> nodeValues,
      IdentityHashMap<Block, TransferInput<V, S>> stores,
      IdentityHashMap<Tree, Set<Node>> treeLookup,
      IdentityHashMap<UnaryTree, BinaryTree> postfixLookup,
      HashMap<VariableElement, V> finalLocalValues) {
    this(nodeValues, stores, treeLookup, postfixLookup, finalLocalValues, new IdentityHashMap<>());
  }

  /**
   * Initialize empty result with specified cache.
   *
   * @param analysisCaches {@link #analysisCaches}
   */
  public AnalysisResult(
      Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
    this(
        new IdentityHashMap<>(),
        new IdentityHashMap<>(),
        new IdentityHashMap<>(),
        new IdentityHashMap<>(),
        new HashMap<>(),
        analysisCaches);
  }

  /**
   * Combine with another analysis result.
   *
   * @param other an analysis result to combine with this
   */
  public void combine(AnalysisResult<V, S> other) {
    copyMapsIfNeeded();
    nodeValues.putAll(other.nodeValues);
    mergeTreeLookup(treeLookup, other.treeLookup);
    postfixLookup.putAll(other.postfixLookup);
    stores.putAll(other.stores);
    finalLocalValues.putAll(other.finalLocalValues);
  }

  /** Make copies of certain internal IdentityHashMaps, if they have not been copied already. */
  private void copyMapsIfNeeded() {
    if (!mapsCopied) {
      nodeValues = new IdentityHashMap<>(nodeValues);
      treeLookup = new IdentityHashMap<>(treeLookup);
      postfixLookup = new IdentityHashMap<>(postfixLookup);
      mapsCopied = true;
    }
  }

  /**
   * Merge all entries from otherTreeLookup into treeLookup. Merge sets if already present.
   *
   * @param treeLookup a map from abstract syntax trees to sets of nodes
   * @param otherTreeLookup another treeLookup that will be merged into {@code treeLookup}
   */
  private static void mergeTreeLookup(
      IdentityHashMap<Tree, Set<Node>> treeLookup,
      IdentityHashMap<Tree, Set<Node>> otherTreeLookup) {
    for (Map.Entry<Tree, Set<Node>> entry : otherTreeLookup.entrySet()) {
      Set<Node> hit = treeLookup.get(entry.getKey());
      if (hit == null) {
        treeLookup.put(entry.getKey(), entry.getValue());
      } else {
        hit.addAll(entry.getValue());
      }
    }
  }

  /**
   * Returns the value of effectively final local variables.
   *
   * @return the value of effectively final local variables
   */
  public HashMap<VariableElement, V> getFinalLocalValues() {
    return finalLocalValues;
  }

  /**
   * Returns the abstract value for {@link Node} {@code n}, or {@code null} if no information is
   * available. Note that if the analysis has not finished yet, this value might not represent the
   * final value for this node.
   *
   * @param n a node
   * @return the abstract value for {@link Node} {@code n}, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(Node n) {
    return nodeValues.get(n);
  }

  /**
   * Returns the abstract value for {@link Tree} {@code t}, or {@code null} if no information is
   * available. Note that if the analysis has not finished yet, this value might not represent the
   * final value for this node.
   *
   * @param t a tree
   * @return the abstract value for {@link Tree} {@code t}, or {@code null} if no information is
   *     available
   */
  public @Nullable V getValue(Tree t) {
    Set<Node> nodes = treeLookup.get(t);

    if (nodes == null) {
      return null;
    }
    V merged = null;
    for (Node aNode : nodes) {
      V a = getValue(aNode);
      if (merged == null) {
        merged = a;
      } else if (a != null) {
        merged = merged.leastUpperBound(a);
      }
    }
    return merged;
  }

  /**
   * Returns the {@code Node}s corresponding to a particular {@code Tree}. Multiple {@code Node}s
   * can correspond to a single {@code Tree} because of several reasons:
   *
   * <ol>
   *   <li>In a lambda expression such as {@code () -> 5} the {@code 5} is both an {@code
   *       IntegerLiteralNode} and a {@code LambdaResultExpressionNode}.
   *   <li>Widening and narrowing primitive conversions can result in {@code WideningConversionNode}
   *       and {@code NarrowingConversionNode}.
   *   <li>Automatic String conversion can result in a {@code StringConversionNode}.
   *   <li>Trees for {@code finally} blocks are cloned to achieve a precise CFG. Any {@code Tree}
   *       within a finally block can have multiple corresponding {@code Node}s attached to them.
   * </ol>
   *
   * Callers of this method should always iterate through the returned set, possibly ignoring all
   * {@code Node}s they are not interested in.
   *
   * @param tree a tree
   * @return the set of {@link Node}s for a given {@link Tree}
   */
  public @Nullable Set<Node> getNodesForTree(Tree tree) {
    return treeLookup.get(tree);
  }

  /**
   * Returns the synthetic {@code v + 1} or {@code v - 1} corresponding to the postfix increment or
   * decrement tree.
   *
   * @param postfixTree a postfix increment or decrement tree
   * @return the synthetic {@code v + 1} or {@code v - 1} corresponding to the postfix increment or
   *     decrement tree
   */
  public BinaryTree getPostfixBinaryTree(UnaryTree postfixTree) {
    if (!postfixLookup.containsKey(postfixTree)) {
      throw new BugInCF(postfixTree + " is not in postfixLookup");
    }
    return postfixLookup.get(postfixTree);
  }

  /**
   * Returns the store immediately before a given {@link Tree}.
   *
   * @param tree a tree
   * @return the store immediately before a given {@link Tree}
   */
  public @Nullable S getStoreBefore(Tree tree) {
    Set<Node> nodes = getNodesForTree(tree);
    if (nodes == null) {
      return null;
    }
    S merged = null;
    for (Node node : nodes) {
      S s = getStoreBefore(node);
      if (merged == null) {
        merged = s;
      } else if (s != null) {
        merged = merged.leastUpperBound(s);
      }
    }
    return merged;
  }

  /**
   * Returns the store immediately before a given {@link Node}.
   *
   * @param node a node
   * @return the store immediately before a given {@link Node}
   */
  public @Nullable S getStoreBefore(Node node) {
    return runAnalysisFor(node, Analysis.BeforeOrAfter.BEFORE);
  }

  /**
   * Returns the regular store immediately before a given {@link Block}.
   *
   * @param block a block
   * @return the store right before the given block
   */
  public S getStoreBefore(Block block) {
    TransferInput<V, S> transferInput = stores.get(block);
    assert transferInput != null : "@AssumeAssertion(nullness): transferInput should be non-null";
    Analysis<V, S, ?> analysis = transferInput.analysis;
    switch (analysis.getDirection()) {
      case FORWARD:
        return transferInput.getRegularStore();
      case BACKWARD:
        Node firstNode;
        switch (block.getType()) {
          case REGULAR_BLOCK:
            firstNode = block.getNodes().get(0);
            break;
          case EXCEPTION_BLOCK:
            firstNode = ((ExceptionBlock) block).getNode();
            break;
          default:
            firstNode = null;
        }
        if (firstNode == null) {
          // This block doesn't contains any node, return the store in the transfer input
          return transferInput.getRegularStore();
        }
        return analysis.runAnalysisFor(
            firstNode, Analysis.BeforeOrAfter.BEFORE, transferInput, nodeValues, analysisCaches);
      default:
        throw new BugInCF("Unknown direction: " + analysis.getDirection());
    }
  }

  /**
   * Returns the regular store immediately after a given block.
   *
   * @param block a block
   * @return the store after the given block
   */
  public S getStoreAfter(Block block) {
    TransferInput<V, S> transferInput = stores.get(block);
    assert transferInput != null : "@AssumeAssertion(nullness): transferInput should be non-null";
    Analysis<V, S, ?> analysis = transferInput.analysis;
    switch (analysis.getDirection()) {
      case FORWARD:
        Node lastNode = block.getLastNode();
        if (lastNode == null) {
          // This block doesn't contain any node, return the store in the transfer input
          return transferInput.getRegularStore();
        }
        return analysis.runAnalysisFor(
            lastNode, Analysis.BeforeOrAfter.AFTER, transferInput, nodeValues, analysisCaches);
      case BACKWARD:
        return transferInput.getRegularStore();
      default:
        throw new BugInCF("Unknown direction: " + analysis.getDirection());
    }
  }

  /**
   * Returns the store immediately after a given {@link Tree}.
   *
   * @param tree a tree
   * @return the store immediately after a given {@link Tree}
   */
  public @Nullable S getStoreAfter(Tree tree) {
    Set<Node> nodes = getNodesForTree(tree);
    if (nodes == null) {
      return null;
    }
    S merged = null;
    for (Node node : nodes) {
      S s = getStoreAfter(node);
      if (merged == null) {
        merged = s;
      } else if (s != null) {
        merged = merged.leastUpperBound(s);
      }
    }
    return merged;
  }

  /**
   * Returns the store immediately after a given {@link Node}.
   *
   * @param node a node
   * @return the store immediately after a given {@link Node}
   */
  public @Nullable S getStoreAfter(Node node) {
    return runAnalysisFor(node, Analysis.BeforeOrAfter.AFTER);
  }

  /**
   * Runs the analysis again within the block of {@code node} and returns the store at the location
   * of {@code node}. If {@code before} is true, then the store immediately before the {@link Node}
   * {@code node} is returned. Otherwise, the store after {@code node} is returned.
   *
   * <p>If the given {@link Node} cannot be reached (in the control flow graph), then {@code null}
   * is returned.
   *
   * @param node the node to analyze
   * @param preOrPost which store to return: the store immediately before {@code node} or the store
   *     after {@code node}
   * @return the store before or after {@code node} (depends on the value of {@code before}) after
   *     running the analysis
   */
  protected @Nullable S runAnalysisFor(Node node, Analysis.BeforeOrAfter preOrPost) {
    // block is null if node is a formal parameter of a method, or is a field access thereof
    Block block = node.getBlock();
    assert block != null : "@AssumeAssertion(nullness): null block for node " + node;
    TransferInput<V, S> transferInput = stores.get(block);
    if (transferInput == null) {
      return null;
    }
    // Calling Analysis.runAnalysisFor() may mutate the internal nodeValues map inside an
    // AbstractAnalysis object, and by default the AnalysisResult constructor just wraps this map
    // without copying it.  So here the AnalysisResult maps must be copied, to preserve them.
    copyMapsIfNeeded();
    return runAnalysisFor(node, preOrPost, transferInput, nodeValues, analysisCaches);
  }

  /**
   * Runs the analysis again within the block of {@code node} and returns the store at the location
   * of {@code node}. If {@code before} is true, then the store immediately before the {@link Node}
   * {@code node} is returned. Otherwise, the store immediately after {@code node} is returned. If
   * {@code analysisCaches} is not null, this method uses a cache. {@code analysisCaches} is a map
   * of a block of node to the cached analysis result. If the cache for {@code transferInput} is not
   * in {@code analysisCaches}, this method creates new cache and stores it in {@code
   * analysisCaches}. The cache is a map of nodes to the analysis results of the nodes.
   *
   * @param <V> the abstract value type to be tracked by the analysis
   * @param <S> the store type used in the analysis
   * @param node the node to analyze
   * @param preOrPost which store to return: the store immediately before {@code node} or the store
   *     after {@code node}
   * @param transferInput a transfer input
   * @param nodeValues {@link #nodeValues}
   * @param analysisCaches {@link #analysisCaches}
   * @return the store before or after {@code node} (depends on the value of {@code before}) after
   *     running the analysis
   */
  public static <V extends AbstractValue<V>, S extends Store<S>> S runAnalysisFor(
      Node node,
      Analysis.BeforeOrAfter preOrPost,
      TransferInput<V, S> transferInput,
      IdentityHashMap<Node, V> nodeValues,
      Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
    if (transferInput.analysis == null) {
      throw new BugInCF("Analysis in transferInput cannot be null.");
    }
    return transferInput.analysis.runAnalysisFor(
        node, preOrPost, transferInput, nodeValues, analysisCaches);
  }

  /**
   * Returns a verbose string representation of this, useful for debugging.
   *
   * @return a string representation of this
   */
  public String toStringDebug() {
    StringJoiner result =
        new StringJoiner(
            String.format("%n  "), String.format("AnalysisResult{%n  "), String.format("%n}"));
    result.add("nodeValues = " + nodeValuesToString(nodeValues));
    result.add("treeLookup = " + treeLookupToString(treeLookup));
    result.add("postfixLookup = " + postfixLookup);
    result.add("finalLocalValues = " + finalLocalValues);
    result.add("stores = " + stores);
    result.add("analysisCaches = " + analysisCaches);
    return result.toString();
  }

  /**
   * Returns a verbose string representation, useful for debugging. The map has the same type as the
   * {@code nodeValues} field.
   *
   * @param <V> the type of values in the map
   * @param nodeValues a map to format
   * @return a printed representation of the given map
   */
  public static <V> String nodeValuesToString(Map<Node, V> nodeValues) {
    if (nodeValues.isEmpty()) {
      return "{}";
    }
    StringJoiner result = new StringJoiner(String.format("%n    "));
    result.add("{");
    for (Map.Entry<Node, V> entry : nodeValues.entrySet()) {
      Node key = entry.getKey();
      result.add(String.format("%s => %s", key.toStringDebug(), entry.getValue()));
    }
    result.add("}");
    return result.toString();
  }

  /**
   * Returns a verbose string representation of a map, useful for debugging. The map has the same
   * type as the {@code treeLookup} field.
   *
   * @param treeLookup a map to format
   * @return a printed representation of the given map
   */
  public static String treeLookupToString(Map<Tree, Set<Node>> treeLookup) {
    if (treeLookup.isEmpty()) {
      return "{}";
    }
    StringJoiner result = new StringJoiner(String.format("%n    "));
    result.add("{");
    for (Map.Entry<Tree, Set<Node>> entry : treeLookup.entrySet()) {
      Tree key = entry.getKey();
      result.add(
          TreeUtils.toStringTruncated(key, 65)
              + " => "
              + Node.nodeCollectionToString(entry.getValue()));
    }
    result.add("}");
    return result.toString();
  }
}
