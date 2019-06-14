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
     * @param analysis the analysis containing information about the program represented by the CFG.
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
     * @param store the Store to visualize
     * @return the String presentation of the value of {@link Store}
     */
    String visualizeStore(S store);

    /**
     * Called by a {@code CFAbstractStore} to visualize the class name before calling the {@code
     * CFAbstractStore#internalVisualize()} method.
     *
     * @param classCanonicalName the canonical name of the class
     * @return the String representation of the header of {@link Store}
     */
    String visualizeStoreHeader(String classCanonicalName);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize a local variable.
     *
     * @param localVar the local variable
     * @param value the value of the local variable
     * @return the String representation of the value of a local variable
     */
    String visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of the current
     * object {@code this} in this Store.
     *
     * @param value the value of the current object {@code this}
     * @return the String representation of the value of current object {@code this}
     */
    String visualizeStoreThisVal(A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of fields
     * collected by this Store.
     *
     * @param fieldAccess the field
     * @param value the value of the field
     * @return the String representation of the value of fields of this {@link Store}
     */
    String visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of arrays
     * collected by this {@link Store}.
     *
     * @param arrayValue the array
     * @param value the value of the array
     * @return the String representation of the value of arrays of this {@link Store}
     */
    String visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of pure method
     * calls collected by this {@link Store}.
     *
     * @param methodCall the pure method call
     * @param value the value of the pure method call
     * @return the String representation of the value of pure method calls of this {@link Store}
     */
    String visualizeStoreMethodVals(FlowExpressions.MethodCall methodCall, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the value of class names
     * collected by this Store.
     *
     * @param className the class name
     * @param value the value of the class name
     * @return the String representation of the value of class names of this {@link Store}
     */
    String visualizeStoreClassVals(FlowExpressions.ClassName className, A value);

    /**
     * Called by {@code CFAbstractStore#internalVisualize()} to visualize the specific information
     * collected according to the specific kind of Store. Currently, these Stores call this method:
     * {@code LockStore}, {@code NullnessStore}, and {@code InitializationStore} to visualize
     * additional information.
     *
     * @param keyName the name of the specific information to be visualized
     * @param value the value of the specific information to be visualized
     * @return the String representation of the specific information
     */
    String visualizeStoreKeyVal(String keyName, Object value);

    /**
     * Called by {@code CFAbstractStore} to visualize any information after the invocation of {@code
     * CFAbstractStore#internalVisualize()}.
     *
     * @return the String representation of the value of footer of this {@link Store}
     */
    String visualizeStoreFooter();

    /**
     * Visualize a {@link Block} based on the analysis.
     *
     * @param bb the {@link Block}
     * @param analysis the current {@link Analysis}
     * @return the String representation of this {@link Block}
     */
    String visualizeBlock(Block bb, @Nullable Analysis<A, S, T> analysis);

    /**
     * Visualize a {@link SpecialBlock}.
     *
     * @param sbb the {@link SpecialBlock}
     * @return the String representation of the type of this {@link SpecialBlock}(entry, exit or
     *     exceptional-exit)
     */
    String visualizeSpecialBlock(SpecialBlock sbb);

    /**
     * Visualize the transferInput of a Block based on the analysis.
     *
     * @param bb the {@link Block}
     * @param analysis the current {@link Analysis}
     * @return the String representation of the transferInput of this {@link Block}
     */
    String visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis);

    /**
     * Visualize a Node based on the analysis.
     *
     * @param t the {@link Node}
     * @param analysis the current {@link Analysis}
     * @return the String representation of this {@link Node}
     */
    String visualizeBlockNode(Node t, @Nullable Analysis<A, S, T> analysis);

    /** Shutdown method called once from the shutdown hook of the {@code BaseTypeChecker}. */
    void shutdown();
}
