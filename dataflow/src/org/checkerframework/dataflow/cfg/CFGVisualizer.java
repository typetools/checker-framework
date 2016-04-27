package org.checkerframework.dataflow.cfg;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Element;

/**
 * Perform some visualization on a control flow graph.
 * The particular operations depend on the implementation.
 */
public interface CFGVisualizer<A extends AbstractValue<A>,
        S extends Store<S>, T extends TransferFunction<A, S>> {
    /**
     * Initialization method guaranteed to be called once before the
     * first invocation of {@link visualize}.
     *
     * @param args Implementation-dependent options.
     */
    void init(Map<String, Object> args);

    /**
     * Output a visualization representing the control flow graph starting
     * at <code>entry</code>.
     * The concrete actions are implementation dependent.
     *
     * An invocation <code>visualize(cfg, entry, null);</code> does not
     * output stores at the beginning of basic blocks.
     *
     * @param cfg
     *            The CFG to visualize.
     * @param entry
     *            The entry node of the control flow graph to be represented.
     * @param analysis
     *            An analysis containing information about the program
     *            represented by the CFG. The information includes {@link Store}s
     *            that are valid at the beginning of basic blocks reachable
     *            from <code>entry</code> and per-node information for value
     *            producing {@link Node}s. Can also be <code>null</code> to
     *            indicate that this information should not be output.
     * @return Possible analysis results, e.g. generated file names.
     */
    /*@Nullable*/ Map<String, Object> visualize(ControlFlowGraph cfg, Block entry,
            /*@Nullable*/ Analysis<A, S, T> analysis);

    /**
     * Delegate the visualization responsibility
     * to the passed {@link Store} instance, which will call back to this
     * visualizer instance for sub-components.
     *
     * @param store The store to visualize.
     */
    void visualizeStore(S store);

    /**
     * Called by a <code>CFAbstractStore</code> to visualize
     * the class name before calling the
     * <code>CFAbstractStore#internalVisualize()</code> method.
     *
     * @param classCanonicalName The canonical name of the class.
     */
    void visualizeStoreHeader(String classCanonicalName);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * a local variable.
     *
     * @param localVar The local variable.
     * @param value The value of the local variable.
     */
    void visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the value of the current object <code>this</code> in this Store.
     *
     * @param value The value of the current object this.
     */
    void visualizeStoreThisVal(A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the value of fields collected by this Store.
     *
     * @param fieldAccess The field.
     * @param value The value of the field.
     */
    void visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the value of arrays collected by this Store.
     *
     * @param arrayValue The array.
     * @param value The value of the array.
     */
    void visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the value of pure method calls collected by this Store.
     *
     * @param methodCall The pure method call.
     * @param value The value of the pure method call.
     */
    void visualizeStoreMethodVals(FlowExpressions.MethodCall methodCall, A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the value of class names collected by this Store.
     *
     * @param className The class name.
     * @param value The value of the class name.
     */
    void visualizeStoreClassVals(FlowExpressions.ClassName className, A value);

    /**
     * Called by <code>CFAbstractStore#internalVisualize()</code> to visualize
     * the specific information collected according to the specific kind of Store.
     * Currently, these Stores call this method: <code>LockStore</code>,
     * <code>NullnessStore</code>, and <code>InitializationStore</code> to visualize additional
     * information.
     *
     * @param keyName The name of the specific information to be visualized.
     * @param value The value of the specific information to be visualized.
     */
    void visualizeStoreKeyVal(String keyName, Object value);

    /**
     * Called by <code>CFAbstractStore</code> to visualize
     * any information after the invocation of <code>CFAbstractStore#internalVisualize()</code>.
     */
    void visualizeStoreFooter();

    /**
     * Visualize a block based on the analysis.
     *
     * @param bb The block.
     * @param analysis The current analysis.
     */
    void visualizeBlock(Block bb, /*@Nullable*/ Analysis<A, S, T> analysis);

    /**
     * Visualize a SpecialBlock.
     *
     * @param sbb The special block.
     */
    void visualizeSpecialBlock(SpecialBlock sbb);

    /**
     * Visualize the transferInput of a Block based on the analysis.
     *
     * @param bb The block.
     * @param analysis The current analysis.
     */
    void visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis);

    /**
     * Visualize a Node based on the analysis.
     *
     * @param t The node.
     * @param analysis The current analysis.
     */
    void visualizeBlockNode(Node t, /*@Nullable*/ Analysis<A, S, T> analysis);

    /**
     * Shutdown method called once from the shutdown hook of the
     * <code>BaseTypeChecker</code>.
     */
    void shutdown();
}
