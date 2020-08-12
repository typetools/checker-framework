package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

/**
 * An {@link AnalysisResult} represents the result of a org.checkerframework.dataflow analysis by
 * providing the abstract values given a node or a tree. Note that it does not keep track of custom
 * results computed by some analysis.
 *
 * @param <V> type of the abstract value that is tracked
 * @param <S> the store type used in the analysis
 */
public class AnalysisResult<V extends AbstractValue<V>, S extends Store<S>> {

    /** Abstract values of nodes. */
    protected final IdentityHashMap<Node, V> nodeValues;

    /**
     * Map from AST {@link Tree}s to sets of {@link Node}s.
     *
     * <p>Some of those Nodes might not be keys in {@link #nodeValues}. One reason is that the Node
     * is unreachable in the control flow graph, so dataflow never gave it a value.
     */
    protected final IdentityHashMap<Tree, Set<Node>> treeLookup;

    /** Map from AST {@link UnaryTree}s to corresponding {@link AssignmentNode}s. */
    protected final IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup;

    /** Map from (effectively final) local variable elements to their abstract value. */
    protected final HashMap<Element, V> finalLocalValues;

    /** The stores before every method call. */
    protected final IdentityHashMap<Block, TransferInput<V, S>> stores;

    /**
     * Caches of the analysis results for each input for the block of the node and each node.
     *
     * @see #runAnalysisFor(Node, boolean, TransferInput, IdentityHashMap, Map)
     */
    protected final Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>>
            analysisCaches;

    /**
     * Initialize with given mappings.
     *
     * @param nodeValues {@link #nodeValues}
     * @param stores {@link #stores}
     * @param treeLookup {@link #treeLookup}
     * @param unaryAssignNodeLookup {@link #unaryAssignNodeLookup}
     * @param finalLocalValues {@link #finalLocalValues}
     * @param analysisCaches {@link #analysisCaches}
     */
    protected AnalysisResult(
            Map<Node, V> nodeValues,
            IdentityHashMap<Block, TransferInput<V, S>> stores,
            IdentityHashMap<Tree, Set<Node>> treeLookup,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup,
            HashMap<Element, V> finalLocalValues,
            Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
        this.nodeValues = new IdentityHashMap<>(nodeValues);
        this.treeLookup = new IdentityHashMap<>(treeLookup);
        this.unaryAssignNodeLookup = new IdentityHashMap<>(unaryAssignNodeLookup);
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
     * @param unaryAssignNodeLookup {@link #unaryAssignNodeLookup}
     * @param finalLocalValues {@link #finalLocalValues}
     */
    public AnalysisResult(
            Map<Node, V> nodeValues,
            IdentityHashMap<Block, TransferInput<V, S>> stores,
            IdentityHashMap<Tree, Set<Node>> treeLookup,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup,
            HashMap<Element, V> finalLocalValues) {
        this(
                nodeValues,
                stores,
                treeLookup,
                unaryAssignNodeLookup,
                finalLocalValues,
                new IdentityHashMap<>());
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
        nodeValues.putAll(other.nodeValues);
        mergeTreeLookup(treeLookup, other.treeLookup);
        unaryAssignNodeLookup.putAll(other.unaryAssignNodeLookup);
        stores.putAll(other.stores);
        finalLocalValues.putAll(other.finalLocalValues);
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
    public HashMap<Element, V> getFinalLocalValues() {
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
     *   <li>Narrowing and widening primitive conversions can result in {@code
     *       NarrowingConversionNode} and {@code WideningConversionNode}.
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
     * Returns the corresponding {@link AssignmentNode} for a given {@link UnaryTree}.
     *
     * @param tree a unary tree
     * @return the corresponding assignment node
     */
    public AssignmentNode getAssignForUnaryTree(UnaryTree tree) {
        if (!unaryAssignNodeLookup.containsKey(tree)) {
            throw new BugInCF(tree + " is not in unaryAssignNodeLookup");
        }
        return unaryAssignNodeLookup.get(tree);
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
        return runAnalysisFor(node, true);
    }

    /**
     * Returns the regular store immediately before a given {@link Block}.
     *
     * @param block a block
     * @return the store right before the given block
     */
    public S getStoreBefore(Block block) {
        TransferInput<V, S> transferInput = stores.get(block);
        assert transferInput != null
                : "@AssumeAssertion(nullness): transferInput should be non-null";
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
                        firstNode, true, transferInput, nodeValues, analysisCaches);
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
        assert transferInput != null
                : "@AssumeAssertion(nullness): transferInput should be non-null";
        Analysis<V, S, ?> analysis = transferInput.analysis;
        switch (analysis.getDirection()) {
            case FORWARD:
                Node lastNode = block.getLastNode();
                if (lastNode == null) {
                    // This block doesn't contain any node, return the store in the transfer input
                    return transferInput.getRegularStore();
                }
                return analysis.runAnalysisFor(
                        lastNode, false, transferInput, nodeValues, analysisCaches);
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
        return runAnalysisFor(node, false);
    }

    /**
     * Runs the analysis again within the block of {@code node} and returns the store at the
     * location of {@code node}. If {@code before} is true, then the store immediately before the
     * {@link Node} {@code node} is returned. Otherwise, the store after {@code node} is returned.
     *
     * <p>If the given {@link Node} cannot be reached (in the control flow graph), then {@code null}
     * is returned.
     *
     * @param node the node to analyze
     * @param before the boolean value to indicate which store to return (if it is true, return the
     *     store immediately before {@code node}; otherwise, the store after {@code node} is
     *     returned)
     * @return the store before or after {@code node} (depends on the value of {@code before}) after
     *     running the analysis
     */
    protected @Nullable S runAnalysisFor(Node node, boolean before) {
        Block block = node.getBlock();
        assert block != null : "@AssumeAssertion(nullness): invariant";
        TransferInput<V, S> transferInput = stores.get(block);
        if (transferInput == null) {
            return null;
        }
        return runAnalysisFor(node, before, transferInput, nodeValues, analysisCaches);
    }

    /**
     * Runs the analysis again within the block of {@code node} and returns the store at the
     * location of {@code node}. If {@code before} is true, then the store immediately before the
     * {@link Node} {@code node} is returned. Otherwise, the store immediately after {@code node} is
     * returned. If {@code analysisCaches} is not null, this method uses a cache. {@code
     * analysisCaches} is a map of a block of node to the cached analysis result. If the cache for
     * {@code transferInput} is not in {@code analysisCaches}, this method creates new cache and
     * stores it in {@code analysisCaches}. The cache is a map of nodes to the analysis results of
     * the nodes.
     *
     * @param <V> the abstract value type to be tracked by the analysis
     * @param <S> the store type used in the analysis
     * @param node the node to analyze
     * @param before the boolean value to indicate which store to return (if it is true, return the
     *     store immediately before {@code node}; otherwise, the store after {@code node} is
     *     returned)
     * @param transferInput a transfer input
     * @param nodeValues {@link #nodeValues}
     * @param analysisCaches {@link #analysisCaches}
     * @return the store before or after {@code node} (depends on the value of {@code before}) after
     *     running the analysis
     */
    public static <V extends AbstractValue<V>, S extends Store<S>> S runAnalysisFor(
            Node node,
            boolean before,
            TransferInput<V, S> transferInput,
            IdentityHashMap<Node, V> nodeValues,
            Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
        if (transferInput.analysis == null) {
            throw new BugInCF("Analysis in transferInput cannot be null.");
        }
        return transferInput.analysis.runAnalysisFor(
                node, before, transferInput, nodeValues, analysisCaches);
    }

    /**
     * Returns a verbose string representation of this, useful for debugging.
     *
     * @return a string representation of this
     */
    public String toStringDebug() {
        StringJoiner result =
                new StringJoiner(
                        String.format("%n  "),
                        String.format("AnalysisResult{%n  "),
                        String.format("%n}"));
        result.add("nodeValues = " + nodeValuesToString(nodeValues));
        result.add("treeLookup = " + treeLookupToString(treeLookup));
        result.add("unaryAssignNodeLookup = " + unaryAssignNodeLookup);
        result.add("finalLocalValues = " + finalLocalValues);
        result.add("stores = " + stores);
        result.add("analysisCaches = " + analysisCaches);
        return result.toString();
    }

    /**
     * Returns a verbose string representation, useful for debugging. The map has the same type as
     * the {@code nodeValues} field.
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
            String treeString = key.toString().replaceAll("[ \n\t]+", " ");
            if (treeString.length() > 65) {
                treeString = "\"" + treeString.substring(0, 60) + "...\"";
            }
            result.add(treeString + " => " + Node.nodeCollectionToString(entry.getValue()));
        }
        result.add("}");
        return result.toString();
    }
}
