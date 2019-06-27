package org.checkerframework.dataflow.cfg;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/** Generate the String representation of a control flow graph. */
public class StringCFGVisualizer<
                A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
        extends AbstractCFGVisualizer<A, S, T> {

    @Override
    public Map<String, Object> visualize(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {
        String stringGraph = visualizeGraph(cfg, entry, analysis);
        Map<String, Object> res = new HashMap<>();
        res.put("stringGraph", stringGraph);
        return res;
    }

    @Override
    public String visualizeNodes(
            Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<A, S, T> analysis) {
        StringBuilder sbStringNodes = new StringBuilder();
        sbStringNodes.append(lineSeparator);

        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // Generate all the Nodes.
        for (Block v : blocks) {
            sbStringNodes.append(v.getId()).append(":").append(lineSeparator);
            if (verbose) {
                sbStringNodes
                        .append(getProcessOrderSimpleString(processOrder.get(v)))
                        .append(lineSeparator);
            }
            String strBlock = visualizeBlock(v, analysis);
            if (strBlock.length() == 0) {
                sbStringNodes.append(lineSeparator);
            } else {
                sbStringNodes.append(strBlock).append(lineSeparator);
            }
        }
        return sbStringNodes.toString();
    }

    @Override
    protected String addEdge(long sId, long eId, String flowRule) {
        if (this.verbose) {
            return sId + " -> " + eId + " " + flowRule + lineSeparator;
        }
        return sId + " -> " + eId + lineSeparator;
    }

    @Override
    public @Nullable String visualizeBlock(Block bb, @Nullable Analysis<A, S, T> analysis) {
        return super.visualizeBlockHelper(bb, analysis, lineSeparator);
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return super.visualizeSpecialBlockHelper(sbb, lineSeparator);
    }

    @Override
    public String visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(bb, analysis, lineSeparator);
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<A, S, T> analysis) {
        StringBuilder sbBlockNode = new StringBuilder();
        sbBlockNode.append(t.toString()).append("   [ ").append(getNodeSimpleName(t)).append(" ]");
        if (analysis != null) {
            A value = analysis.getValue(t);
            if (value != null) {
                sbBlockNode.append(" > ").append(value.toString());
            }
        }
        return sbBlockNode.toString();
    }

    @Override
    public String visualizeStoreThisVal(A value) {
        return storeEntryIndent + "this > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value) {
        return storeEntryIndent + localVar + " > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value) {
        return storeEntryIndent + fieldAccess + " > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value) {
        return storeEntryIndent + arrayValue + " > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreMethodVals(FlowExpressions.MethodCall methodCall, A value) {
        return storeEntryIndent + methodCall + " > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreClassVals(FlowExpressions.ClassName className, A value) {
        return storeEntryIndent + className + " > " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreKeyVal(String keyName, Object value) {
        return storeEntryIndent + keyName + " = " + value + lineSeparator;
    }

    @Override
    public String visualizeStoreHeader(String classCanonicalName) {
        return classCanonicalName + " (" + lineSeparator;
    }

    @Override
    public String visualizeStoreFooter() {
        return ")";
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
