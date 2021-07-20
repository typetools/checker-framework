package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This interface defines a dataflow analysis, given a control flow graph and a transfer function. A
 * dataflow analysis has a direction, either forward or backward. The direction of corresponding
 * transfer function is consistent with the analysis, i.e. a forward analysis has a forward transfer
 * function, and a backward analysis has a backward transfer function.
 *
 * @param <V> the abstract value type to be tracked by the analysis
 * @param <S> the store type used in the analysis
 * @param <T> the transfer function type that is used to approximated runtime behavior
 */
public interface Analysis<
        V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>> {

    /** The direction of an analysis instance. */
    enum Direction {
        /** The forward direction. */
        FORWARD,
        /** The backward direction. */
        BACKWARD
    }

    /**
     * In calls to {@code Analysis#runAnalysisFor}, whether to return the store before or after the
     * given node.
     */
    public static enum BeforeOrAfter {
        /** Return the pre-store. */
        BEFORE,
        /** Return the post-store. */
        AFTER
    }

    /**
     * Get the direction of this analysis.
     *
     * @return the direction of this analysis
     */
    Direction getDirection();

    /**
     * Is the analysis currently running?
     *
     * @return true if the analysis is running currently, else false
     */
    boolean isRunning();

    /**
     * Perform the actual analysis.
     *
     * @param cfg the control flow graph
     */
    void performAnalysis(ControlFlowGraph cfg);

    /**
     * Perform the actual analysis on one block.
     *
     * @param b the block to analyze
     */
    void performAnalysisBlock(Block b);

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
     * @param node the node to analyze
     * @param preOrPost which store to return: the store immediately before {@code node} or the
     *     store after {@code node}
     * @param blockTransferInput the transfer input of the block of this node
     * @param nodeValues abstract values of nodes
     * @param analysisCaches caches of analysis results
     * @return the store before or after {@code node} (depends on the value of {@code before}) after
     *     running the analysis
     */
    S runAnalysisFor(
            Node node,
            Analysis.BeforeOrAfter preOrPost,
            TransferInput<V, S> blockTransferInput,
            IdentityHashMap<Node, V> nodeValues,
            Map<TransferInput<V, S>, IdentityHashMap<Node, TransferResult<V, S>>> analysisCaches);

    /**
     * The result of running the analysis. This is only available once the analysis finished
     * running.
     *
     * @return the result of running the analysis
     */
    AnalysisResult<V, S> getResult();

    /**
     * Get the transfer function of this analysis.
     *
     * @return the transfer function of this analysis
     */
    @Nullable T getTransferFunction();

    /**
     * Get the transfer input of a given {@link Block} b.
     *
     * @param b a given Block
     * @return the transfer input of this Block
     */
    @Nullable TransferInput<V, S> getInput(Block b);

    /**
     * Returns the abstract value for {@link Node} {@code n}, or {@code null} if no information is
     * available. Note that if the analysis has not finished yet, this value might not represent the
     * final value for this node.
     *
     * @param n n a node
     * @return the abstract value for node {@code n}, or {@code null} if no information is available
     */
    @Nullable V getValue(Node n);

    /**
     * Return the abstract value for {@link Tree} {@code t}, or {@code null} if no information is
     * available. Note that if the analysis has not finished yet, this value might not represent the
     * final value for this node.
     *
     * @param t the given tree
     * @return the abstract value for the given tree
     */
    @Nullable V getValue(Tree t);

    /**
     * Returns the regular exit store, or {@code null}, if there is no such store (because the
     * method cannot exit through the regular exit block).
     *
     * @return the regular exit store, or {@code null}, if there is no such store (because the
     *     method cannot exit through the regular exit block)
     */
    @Nullable S getRegularExitStore();

    /**
     * Returns the exceptional exit store.
     *
     * @return the exceptional exit store
     */
    @Nullable S getExceptionalExitStore();
}
