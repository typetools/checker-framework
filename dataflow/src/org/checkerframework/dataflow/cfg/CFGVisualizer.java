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
 * The particular operations depend on the particular implementation.
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
     *            represented by the CFG. The information includes {@link Store}
     *            s that are valid at the beginning of basic blocks reachable
     *            from <code>entry</code> and per-node information for value
     *            producing {@link Node}s. Can also be <code>null</code> to
     *            indicate that this information should not be output.
     * @return Possible analysis results, e.g. generated files.
     */
    /*@Nullable*/ Map<String, Object> visualize(ControlFlowGraph cfg, Block entry,
            /*@Nullable*/ Analysis<A, S, T> analysis);

    /** This is a double-direction delegate pattern interface.
     * this method would delegate the visualize responsibility
     * to the passed {@link Store} instance.
     * Then the store would do something and delegate the 
     * visualize responsibility back to the CFGVisualizer.
     * @param store
     */
    void visualizeStore(S store);

    /**A delegate method called by a {@link CFAbstractStore} to visualize 
     * the class CanonicalName before calling the internalVisualize() method of CFAbstractStore.
     * @param classCanonicalName
     */
    void visualizeStoreHeader(String classCanonicalName);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize its Local Variable
     * @param localVar
     * @param value
     */
    void visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize the Info of Current Object in this Store
     * @param value
     */
    void visualizeStoreThisVal(A value);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize the Info of fields collected by this Store
     * @param fieldAccess
     * @param value
     */
    void visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize the Info of Arrays collected by this Store
     * @param arrayValue
     * @param value
     */
    void visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize 
     * the Info of pure method calls collected by this Store
     * @param methodCall
     * @param value
     */
    void visualizeStoreMethodVals(FlowExpressions.PureMethodCall methodCall, A value);

    /**This method is called by internalVisualize() of CFAbstractStore to visualize the Info of class values collected by this Store
     * @param className
     * @param value
     */
    void visualizeStoreClassVals(FlowExpressions.ClassName className, A value);

    /**This method is called by internalVisualize() of subclasses of CFAbstractStore to visualize
     * the specific Info collected according to the specific kind of Store 
     * current these Stores call this method: {@link LockStore}, {@link NullnessStore}, and {@link InitializationStore}
     * @param keyName the name of the specific Info to be visualize
     * @param value the value of the specific Info to be visualize
     */
    void visualizeStoreKeyVal(String keyName, Object value);

    /**A delegate method called by a {@link CFAbstractStore} to visualize
     * any info need to be visualize after the internalVisualize() method of CFAbstractStore.
     */
    void visualizeStoreFooter();

    /**
     * Visualize a block based on the analysis
     * @param bb
     * @param analysis
     */
    void visualizeBlock(Block bb, /*@Nullable*/ Analysis<A, S, T> analysis);

    /**
     * Visualize a specialBlock
     * @param sbb
     */
    void visualizeSpecialBlock(SpecialBlock sbb);

    /**
     * Visualize the transferInput of a Block based on analysis
     * @param bb
     * @param analysis
     */
    void visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis);

    /**
     * visualized a blockNode based on analysis
     * @param t
     * @param analysis
     */
    void visualizeBlockNode(Node t, /*@Nullable*/ Analysis<A, S, T> analysis);
    /** Shutdown method called once from the shutdown hook of the BaseTypeChecker.
     */

    void shutdown();
}
