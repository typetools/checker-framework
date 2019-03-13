package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * An {@link AnalysisResult} represents the result of a org.checkerframework.dataflow analysis by
 * providing the abstract values given a node or a tree. Note that it does not keep track of custom
 * results computed by some analysis.
 *
 * @param <A> type of the abstract value that is tracked
 */
public class AnalysisResult<A extends AbstractValue<A>, S extends Store<S>> {

    /** Abstract values of nodes. */
    protected final IdentityHashMap<Node, A> nodeValues;

    /** Map from AST {@link Tree}s to sets of {@link Node}s. */
    protected final IdentityHashMap<Tree, Set<Node>> treeLookup;

    /** Map from AST {@link UnaryTree}s to corresponding {@link AssignmentNode}s. */
    protected final IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup;

    /** Map from (effectively final) local variable elements to their abstract value. */
    protected final HashMap<Element, A> finalLocalValues;

    /** The stores before every method call. */
    protected final IdentityHashMap<Block, TransferInput<A, S>> stores;

    /**
     * Caches of the analysis results for each input for the block of the node and each node.
     *
     * @see #runAnalysisFor(Node, boolean, TransferInput, IdentityHashMap, Map)
     */
    protected final Map<TransferInput<A, S>, IdentityHashMap<Node, TransferResult<A, S>>>
            analysisCaches;

    /** Initialize with given mappings. */
    protected AnalysisResult(
            Map<Node, A> nodeValues,
            IdentityHashMap<Block, TransferInput<A, S>> stores,
            IdentityHashMap<Tree, Set<Node>> treeLookup,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup,
            HashMap<Element, A> finalLocalValues,
            Map<TransferInput<A, S>, IdentityHashMap<Node, TransferResult<A, S>>> analysisCaches) {
        this.nodeValues = new IdentityHashMap<>(nodeValues);
        this.treeLookup = new IdentityHashMap<>(treeLookup);
        this.unaryAssignNodeLookup = new IdentityHashMap<>(unaryAssignNodeLookup);
        // TODO: why are stores and finalLocalValues captured?
        this.stores = stores;
        this.finalLocalValues = finalLocalValues;
        this.analysisCaches = analysisCaches;
    }

    /** Initialize with given mappings and empty cache. */
    public AnalysisResult(
            Map<Node, A> nodeValues,
            IdentityHashMap<Block, TransferInput<A, S>> stores,
            IdentityHashMap<Tree, Set<Node>> treeLookup,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup,
            HashMap<Element, A> finalLocalValues) {
        this(
                nodeValues,
                stores,
                treeLookup,
                unaryAssignNodeLookup,
                finalLocalValues,
                new IdentityHashMap<>());
    }

    /** Initialize empty result with specified cache. */
    public AnalysisResult(
            Map<TransferInput<A, S>, IdentityHashMap<Node, TransferResult<A, S>>> analysisCaches) {
        this(
                new IdentityHashMap<>(),
                new IdentityHashMap<>(),
                new IdentityHashMap<>(),
                new IdentityHashMap<>(),
                new HashMap<>(),
                analysisCaches);
    }

    /** Combine with another analysis result. */
    public void combine(AnalysisResult<A, S> other) {
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
        for (Entry<Tree, Set<Node>> entry : otherTreeLookup.entrySet()) {
            Set<Node> hit = treeLookup.get(entry.getKey());
            if (hit == null) {
                treeLookup.put(entry.getKey(), entry.getValue());
            } else {
                hit.addAll(entry.getValue());
            }
        }
    }

    /** @return the value of effectively final local variables */
    public HashMap<Element, A> getFinalLocalValues() {
        return finalLocalValues;
    }

    /**
     * @return the abstract value for {@link Node} {@code n}, or {@code null} if no information is
     *     available.
     */
    public @Nullable A getValue(Node n) {
        return nodeValues.get(n);
    }

    /**
     * @return the abstract value for {@link Tree} {@code t}, or {@code null} if no information is
     *     available.
     */
    public @Nullable A getValue(Tree t) {
        Set<Node> nodes = treeLookup.get(t);

        if (nodes == null) {
            return null;
        }
        A merged = null;
        for (Node aNode : nodes) {
            A a = getValue(aNode);
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

    /** @return the corresponding {@link AssignmentNode} for a given {@link UnaryTree}. */
    public AssignmentNode getAssignForUnaryTree(UnaryTree tree) {
        assert unaryAssignNodeLookup.containsKey(tree) : tree + " is not in unaryAssignNodeLookup";
        return unaryAssignNodeLookup.get(tree);
    }

    /** @return the store immediately before a given {@link Tree}. */
    public S getStoreBefore(Tree tree) {
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

    /** @return the store immediately before a given {@link Node}. */
    public S getStoreBefore(Node node) {
        return runAnalysisFor(node, true);
    }

    /** @return the store immediately after a given {@link Tree}. */
    public S getStoreAfter(Tree tree) {
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

    /** @return the store immediately after a given {@link Node}. */
    public S getStoreAfter(Node node) {
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
    protected S runAnalysisFor(Node node, boolean before) {
        Block block = node.getBlock();
        TransferInput<A, S> transferInput = stores.get(block);
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
     */
    public static <A extends AbstractValue<A>, S extends Store<S>> S runAnalysisFor(
            Node node,
            boolean before,
            TransferInput<A, S> transferInput,
            IdentityHashMap<Node, A> nodeValues,
            Map<TransferInput<A, S>, IdentityHashMap<Node, TransferResult<A, S>>> analysisCaches) {
        assert node != null;
        Block block = node.getBlock();
        assert transferInput != null;
        Analysis<A, S, ?> analysis = transferInput.analysis;
        Node oldCurrentNode = analysis.currentNode;

        // Prepare cache
        IdentityHashMap<Node, TransferResult<A, S>> cache;
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
                        TransferInput<A, S> store = transferInput;
                        TransferResult<A, S> transferResult = null;
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
                        assert false;
                        return null;
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
                        TransferResult<A, S> transferResult =
                                analysis.callTransferFunction(node, transferInput);
                        return transferResult.getRegularStore();
                    }

                default:
                    // Only regular blocks and exceptional blocks can hold nodes.
                    assert false;
                    break;
            }

            return null;
        } finally {
            analysis.currentNode = oldCurrentNode;
            analysis.isRunning = false;
        }
    }
}
