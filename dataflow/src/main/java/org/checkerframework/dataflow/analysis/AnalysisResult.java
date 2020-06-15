package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
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

    /** Map from AST {@link Tree}s to sets of {@link Node}s. */
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

    // Merge all entries from otherTreeLookup into treeLookup. Merge sets if already present.
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
     * available.
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
     * available.
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
     * @return the set of {@link Node}s for a given {@link Tree}
     */
    public @Nullable Set<Node> getNodesForTree(Tree tree) {
        return treeLookup.get(tree);
    }

    /**
     * Return the corresponding {@link AssignmentNode} for a given {@link UnaryTree}.
     *
     * @param tree a unary tree
     * @return the corresponding assignment node
     */
    public AssignmentNode getAssignForUnaryTree(UnaryTree tree) {
        if (!unaryAssignNodeLookup.containsKey(tree)) {
            throw new Error(tree + " is not in unaryAssignNodeLookup");
        }
        return unaryAssignNodeLookup.get(tree);
    }

    /**
     * Returns the store immediately before a given {@link Tree}.
     *
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
     * @return the store immediately before a given {@link Node}
     */
    public @Nullable S getStoreBefore(Node node) {
        return runAnalysisFor(node, true);
    }

    /**
     * Returns the store immediately after a given {@link Tree}.
     *
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
     * {@link Node} {@code node} is returned. Otherwise, the store after {@code node} is returned.
     * If {@code analysisCaches} is not null, this method uses a cache. {@code analysisCaches} is a
     * map to a cache for analysis result from an input of the block of the node. If the cache for
     * {@code transferInput} is not in {@code analysisCaches}, this method create new cache and
     * store it in {@code analysisCaches}. The cache is a map from a node to the analysis result of
     * the node.
     *
     * @param <V> the abstract value type to be tracked by the analysis
     * @param <S> the store type used in the analysis
     * @param node a node
     * @param before indicate before or after the node
     * @param transferInput a transfer input
     * @param nodeValues {@link #nodeValues}
     * @param analysisCaches {@link #analysisCaches}
     * @return store immediately before or after the given node
     */
    public static <V extends AbstractValue<V>, S extends Store<S>> S runAnalysisFor(
            Node node,
            boolean before,
            TransferInput<V, S> transferInput,
            IdentityHashMap<Node, V> nodeValues,
            Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches) {
        assert node != null;
        Block block = node.getBlock();
        assert block != null : "@AssumeAssertion(nullness): invariant";
        assert transferInput != null;
        Analysis<V, S, ?> analysis = transferInput.analysis;
        Node oldCurrentNode = analysis.currentNode;

        // Prepare cache
        IdentityHashMap<Node, TransferResult<V, S>> cache;
        if (analysisCaches != null) {
            cache = analysisCaches.get(transferInput);
            if (cache == null) {
                cache = new IdentityHashMap<>();
                analysisCaches.put(transferInput, cache);
            }
        } else {
            cache = null;
        }

        if (analysis.isRunning) {
            assert analysis.currentInput != null : "@AssumeAssertion(nullness): invariant";
            return analysis.currentInput.getRegularStore();
        }
        analysis.setNodeValues(nodeValues);
        analysis.isRunning = true;
        try {
            switch (block.getType()) {
                case REGULAR_BLOCK:
                    {
                        RegularBlock rb = (RegularBlock) block;

                        // Apply transfer function to contents until we found the node we are
                        // looking for.
                        TransferInput<V, S> store = transferInput;
                        TransferResult<V, S> transferResult = null;
                        for (Node n : rb.getContents()) {
                            analysis.currentNode = n;
                            if (n == node && before) {
                                return store.getRegularStore();
                            }
                            if (cache != null && cache.containsKey(n)) {
                                transferResult = cache.get(n);
                            } else {
                                // Copy the store not to change the state in the cache
                                transferResult = analysis.callTransferFunction(n, store.copy());
                                if (cache != null) {
                                    cache.put(n, transferResult);
                                }
                            }
                            if (n == node) {
                                return transferResult.getRegularStore();
                            }
                            store = new TransferInput<>(n, analysis, transferResult);
                        }
                        // This point should never be reached. If the block of 'node' is
                        // 'block', then 'node' must be part of the contents of 'block'.
                        throw new BugInCF("Unexpected code");
                    }

                case EXCEPTION_BLOCK:
                    {
                        ExceptionBlock eb = (ExceptionBlock) block;

                        // apply transfer function to content
                        assert eb.getNode() == node;
                        if (before) {
                            return transferInput.getRegularStore();
                        }
                        analysis.currentNode = node;
                        TransferResult<V, S> transferResult =
                                analysis.callTransferFunction(node, transferInput);
                        return transferResult.getRegularStore();
                    }

                default:
                    // Only regular blocks and exceptional blocks can hold nodes.
                    throw new BugInCF("Unexpected code");
            }
        } finally {
            analysis.currentNode = oldCurrentNode;
            analysis.isRunning = false;
        }
    }
}
