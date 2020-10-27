package org.checkerframework.dataflow.cfg.visualize;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.visualize.AbstractCFGVisualizer.VisualizeWhere;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;

/** Generate the String representation of a control flow graph. */
public class StringCFGVisualizer<
                V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
        extends AbstractCFGVisualizer<V, S, T> {

    @Override
    public String getSeparator() {
        return "\n";
    }

    @Override
    public Map<String, Object> visualize(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {
        String stringGraph = visualizeGraph(cfg, entry, analysis);
        Map<String, Object> res = new HashMap<>();
        res.put("stringGraph", stringGraph);
        return res;
    }

    @SuppressWarnings("keyfor:enhancedfor.type.incompatible")
    @Override
    public String visualizeNodes(
            Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<V, S, T> analysis) {
        StringJoiner sjStringNodes = new StringJoiner(lineSeparator);
        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // Generate all the Nodes.
        for (@KeyFor("processOrder") Block v : blocks) {
            sjStringNodes.add(v.getUid() + ":");
            if (verbose) {
                sjStringNodes.add(getProcessOrderSimpleString(processOrder.get(v)));
            }
            sjStringNodes.add(visualizeBlock(v, analysis));
            sjStringNodes.add("");
        }

        return sjStringNodes.toString().trim();
    }

    @Override
    protected String visualizeEdge(Object sId, Object eId, String flowRule) {
        if (this.verbose) {
            return sId + " -> " + eId + " " + flowRule;
        }
        return sId + " -> " + eId;
    }

    @Override
    public String visualizeBlock(Block bb, @Nullable Analysis<V, S, T> analysis) {
        return super.visualizeBlockHelper(bb, analysis, lineSeparator).trim();
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return super.visualizeSpecialBlockHelper(sbb);
    }

    @Override
    public String visualizeConditionalBlock(ConditionalBlock cbb) {
        return "ConditionalBlock: then: "
                + cbb.getThenSuccessor().getUid()
                + ", else: "
                + cbb.getElseSuccessor().getUid();
    }

    @Override
    public String visualizeBlockTransferInputBefore(Block bb, Analysis<V, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(
                VisualizeWhere.BEFORE, bb, analysis, lineSeparator);
    }

    @Override
    public String visualizeBlockTransferInputAfter(Block bb, Analysis<V, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(
                VisualizeWhere.AFTER, bb, analysis, lineSeparator);
    }

    @Override
    protected String format(Object obj) {
        return obj.toString();
    }

    @Override
    public String visualizeStoreThisVal(V value) {
        return storeEntryIndent + "this > " + value;
    }

    @Override
    public String visualizeStoreLocalVar(LocalVariable localVar, V value) {
        return storeEntryIndent + localVar + " > " + value;
    }

    @Override
    public String visualizeStoreFieldVal(FieldAccess fieldAccess, V value) {
        return storeEntryIndent + fieldAccess + " > " + value;
    }

    @Override
    public String visualizeStoreArrayVal(ArrayAccess arrayValue, V value) {
        return storeEntryIndent + arrayValue + " > " + value;
    }

    @Override
    public String visualizeStoreMethodVals(MethodCall methodCall, V value) {
        return storeEntryIndent + methodCall + " > " + value;
    }

    @Override
    public String visualizeStoreClassVals(ClassName className, V value) {
        return storeEntryIndent + className + " > " + value;
    }

    @Override
    public String visualizeStoreKeyVal(String keyName, Object value) {
        return storeEntryIndent + keyName + " = " + value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>StringCFGVisualizer does not write into file, so left intentionally blank.
     */
    @Override
    public void shutdown() {}

    /**
     * {@inheritDoc}
     *
     * <p>StringCFGVisualizer does not need a specific header, so just return an empty string.
     */
    @Override
    protected String visualizeGraphHeader() {
        return "";
    }

    /**
     * {@inheritDoc}
     *
     * <p>StringCFGVisualizer does not need a specific footer, so just return an empty string.
     */
    @Override
    protected String visualizeGraphFooter() {
        return "";
    }
}
