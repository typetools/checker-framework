package org.checkerframework.dataflow.cfg;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * Perform some visualization on a control flow graph. The particular operations depend on the
 * implementation.
 */
public interface CFGVisualizer<
        A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>> {
    /**
     * Initialization method guaranteed to be called once before the first invocation of {@link
     * #visualize}.
     *
     * @param args implementation-dependent options
     */
    void init(Map<String, Object> args);

    /**
     * Output a visualization representing the control flow graph starting at {@code entry}. The
     * concrete actions are implementation dependent.
     *
     * <p>An invocation {@code visualize(cfg, entry, null);} does not output stores at the beginning
     * of basic blocks.
     *
     * @param cfg the CFG to visualize
     * @param entry the entry node of the control flow graph to be represented
     * @param analysis an analysis containing information about the program represented by the CFG.
     *     The information includes {@link Store}s that are valid at the beginning of basic blocks
     *     reachable from {@code entry} and per-node information for value producing {@link Node}s.
     *     Can also be {@code null} to indicate that this information should not be output.
     * @return possible analysis results, e.g. generated file names ({@link DOTCFGVisualizer}) and
     *     String representation of CFG ({@link StringCFGVisualizer})
     */
    @Nullable Map<String, Object> visualize(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis);

    /**
     * Delegate the visualization responsibility to the passed {@link Store} instance, which will
     * call back to this visualizer instance for sub-components.
     *
     * @param store the store to visualize
     * @return the String representation of the store {@code store}
     */
    String visualizeStore(S store);

    /**
     * Called by a {@code CFAbstractStore} to visualize the class name before calling the {@code
     * CFAbstractStore#internalVisualize()} method.
     *
     * @param classCanonicalName the canonical name of the class
     * @return the String representation of the header to use for a store with the given canonical
     *     name
     */
    String visualizeStoreHeader(String classCanonicalName);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize a local variable in the
     * given store.
     *
     * @param localVar the local variable
     * @param value the value of the local variable
     * @return the String representation of the local variable in the given store
     */
    String visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of {@code this}
     * in the given store.
     *
     * @param value the value of {@code this}
     * @return the String representation of the value of {@code this} in the given store
     */
    String visualizeStoreThisVal(A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of fields
     * collected by the given store.
     *
     * @param fieldAccess the field
     * @param value the value of the field
     * @return the String representation of the fields collected by the given store
     */
    String visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of arrays
     * collected by the given store.
     *
     * @param arrayValue the array
     * @param value the value of the array
     * @return the String representation of the arrays collected by the given store
     */
    String visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of pure method
     * calls collected by the given store.
     *
     * @param methodCall the pure method call
     * @param value the value of the pure method call
     * @return the String representation of the pure method calls collected by the given store
     */
    String visualizeStoreMethodVals(FlowExpressions.MethodCall methodCall, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of class names
     * collected by the given store.
     *
     * @param className the class name
     * @param value the value of the class name
     * @return the String representation of the class names collected by the given store
     */
    String visualizeStoreClassVals(FlowExpressions.ClassName className, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the specific information
     * collected according to the specific kind of store. Currently, these Stores call this method:
     * {@code LockStore}, {@code NullnessStore}, and {@code InitializationStore} to visualize
     * additional information.
     *
     * @param keyName the name of the specific information to be visualized
     * @param value the value of the specific information to be visualized
     * @return the String representation of the specific information according to the specific kind
     *     of the given store
     */
    String visualizeStoreKeyVal(String keyName, Object value);

    /**
     * Called by {@code CFAbstractStore} to visualize any information after the invocation of {@code
     * CFAbstractStore#internalVisualize()}.
     *
     * @return the String representation of the footer to use for a store
     */
    String visualizeStoreFooter();

    /**
     * Visualize a block based on the analysis.
     *
     * @param bb the block
     * @param analysis the current analysis
     * @return the String representation of the given block {@code bb}
     */
    String visualizeBlock(Block bb, @Nullable Analysis<A, S, T> analysis);

    /**
     * Visualize a special block.
     *
     * @param sbb the special block to visualize
     * @return the String representation of the type of the special block {@code sbb} (entry, exit
     *     or exceptional-exit)
     */
    String visualizeSpecialBlock(SpecialBlock sbb);

    /**
     * Visualize the transferInput of a Block based on the analysis.
     *
     * @param bb the block
     * @param analysis the current analysis
     * @return the String representation of the transferInput of the block {@code bb}
     */
    String visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis);

    /**
     * Visualize a Node based on the analysis.
     *
     * @param t the node
     * @param analysis the current analysis
     * @return the String representation of the node {@code t}
     */
    String visualizeBlockNode(Node t, @Nullable Analysis<A, S, T> analysis);

    /** Shutdown method called once from the shutdown hook of the {@code BaseTypeChecker}. */
    void shutdown();
}
