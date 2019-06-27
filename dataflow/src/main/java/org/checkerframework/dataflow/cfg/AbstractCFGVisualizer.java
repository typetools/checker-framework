package org.checkerframework.dataflow.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * This abstract class makes implementing a {@link CFGVisualizer} easier. Some of the methods in
 * {@link CFGVisualizer} are already implemented in this abstract class, but can be overridden if
 * necessary.
 *
 * @see DOTCFGVisualizer
 * @see StringCFGVisualizer
 */
public abstract class AbstractCFGVisualizer<
                A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
        implements CFGVisualizer<A, S, T> {

    /**
     * Initialized in {@link #init(Map)}. If its value is {@code true}, {@link CFGVisualizer}
     * returns more detailed information.
     */
    protected boolean verbose;

    /** The line separator. */
    protected final String lineSeparator = System.lineSeparator();

    /** The indentation for elements of the store. */
    protected final String storeEntryIndent = "  ";

    @Override
    public void init(Map<String, Object> args) {
        Object verb = args.get("verbose");
        this.verbose =
                verb != null
                        && (verb instanceof String
                                ? Boolean.getBoolean((String) verb)
                                : (boolean) verb);
    }

    /**
     * Visualize a control flow graph.
     *
     * @param cfg the current control flow graph
     * @param entry the entry block of the control flow graph
     * @param analysis the current analysis
     * @return the representation of the control flow graph
     */
    protected String visualizeGraph(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {
        return visualizeGraphHeader()
                + visualizeGraphWithoutHeaderAndFooter(cfg, entry, analysis)
                + visualizeGraphFooter();
    }

    /**
     * Helper method to visualize a control flow graph, without outputting a header or footer.
     *
     * @param cfg the control flow graph
     * @param entry the entry block of the control flow graph
     * @param analysis the current analysis
     * @return the String representation of the control flow graph
     */
    protected String visualizeGraphWithoutHeaderAndFooter(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {
        Set<Block> visited = new HashSet<>();
        StringBuilder sbGraph = new StringBuilder();
        Queue<Block> workList = new ArrayDeque<>();
        Block cur = entry;
        visited.add(entry);
        while (cur != null) {
            handleSuccessorsHelper(cur, visited, workList, sbGraph);
            cur = workList.poll();
        }
        sbGraph.append(visualizeNodes(visited, cfg, analysis));
        return sbGraph.toString();
    }

    /**
     * Adds the successors of the current block to the work list and the visited blocks list.
     *
     * @param cur the current block
     * @param visited the set of blocks that have already been visited or are in the work list
     * @param workList the queue of blocks to be processed
     * @param sbGraph the {@link StringBuilder} to store the graph
     */
    protected void handleSuccessorsHelper(
            Block cur, Set<Block> visited, Queue<Block> workList, StringBuilder sbGraph) {
        if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
            ConditionalBlock ccur = ((ConditionalBlock) cur);
            Block thenSuccessor = ccur.getThenSuccessor();
            sbGraph.append(
                    addEdge(
                            ccur.getId(),
                            thenSuccessor.getId(),
                            ccur.getThenFlowRule().toString()));
            addBlock(thenSuccessor, visited, workList);
            Block elseSuccessor = ccur.getElseSuccessor();
            sbGraph.append(
                    addEdge(
                            ccur.getId(),
                            elseSuccessor.getId(),
                            ccur.getElseFlowRule().toString()));
            addBlock(elseSuccessor, visited, workList);
        } else {
            SingleSuccessorBlock sscur = (SingleSuccessorBlock) cur;
            Block succ = sscur.getSuccessor();
            if (succ != null) {
                sbGraph.append(addEdge(cur.getId(), succ.getId(), sscur.getFlowRule().name()));
                addBlock(succ, visited, workList);
            }
        }
        if (cur.getType() == Block.BlockType.EXCEPTION_BLOCK) {
            ExceptionBlock ecur = (ExceptionBlock) cur;
            for (Map.Entry<TypeMirror, Set<Block>> e : ecur.getExceptionalSuccessors().entrySet()) {
                TypeMirror cause = e.getKey();
                String exception = cause.toString();
                if (exception.startsWith("java.lang.")) {
                    exception = exception.replace("java.lang.", "");
                }
                for (Block b : e.getValue()) {
                    sbGraph.append(addEdge(cur.getId(), b.getId(), exception));
                    addBlock(b, visited, workList);
                }
            }
        }
    }

    /**
     * Checks whether a block exists in the visited blocks list, and, if not, adds it to the visited
     * blocks list and the work list.
     *
     * @param b the block to check
     * @param visited the set of blocks that have already been visited or are in the work list
     * @param workList the queue of blocks to be processed
     */
    protected void addBlock(Block b, Set<Block> visited, Queue<Block> workList) {
        if (!visited.contains(b)) {
            visited.add(b);
            workList.add(b);
        }
    }

    /**
     * Helper method to visualize a block.
     *
     * @param bb the block
     * @param analysis the current analysis
     * @param escapeString the escape String for the special need of visualization, e.g., "\\l" for
     *     {@link DOTCFGVisualizer} to keep line left-justification, "\n" for {@link
     *     StringCFGVisualizer} to simply add a new line
     * @return the String representation of the block
     */
    protected String visualizeBlockHelper(
            Block bb, @Nullable Analysis<A, S, T> analysis, String escapeString) {
        StringBuilder sbBlock = new StringBuilder();
        sbBlock.append(loopOverBlockContents(bb, analysis, escapeString));

        // Handle case where no contents are present.
        boolean centered = false;
        if (sbBlock.length() == 0) {
            if (bb.getType() == Block.BlockType.SPECIAL_BLOCK) {
                sbBlock.append(visualizeSpecialBlock((SpecialBlock) bb));
                centered = true;
            } else {
                return "";
            }
        }

        // Visualize transfer input if necessary.
        if (analysis != null) {
            // The transfer input before this block is added before the block content.
            sbBlock.insert(0, visualizeBlockTransferInput(bb, analysis));
            if (verbose) {
                Node lastNode = getLastNode(bb);
                if (lastNode != null) {
                    StringBuilder sbStore = new StringBuilder();
                    sbStore.append(escapeString).append("~~~~~~~~~").append(escapeString);
                    sbStore.append("After: ");
                    sbStore.append(visualizeStore(analysis.getResult().getStoreAfter(lastNode)));
                    sbBlock.append(sbStore);
                }
            }
        }
        if (!centered) {
            sbBlock.append(escapeString);
        }
        return sbBlock.toString();
    }

    /**
     * Iterates over the block content and visualizes all the nodes in it.
     *
     * @param bb the block
     * @param analysis the current analysis
     * @param separator the separator between the nodes of the block
     * @return the String representation of the contents of the block
     */
    protected String loopOverBlockContents(
            Block bb, @Nullable Analysis<A, S, T> analysis, String separator) {

        List<Node> contents = addBlockContent(bb);
        StringJoiner sjBlockContents = new StringJoiner(separator);
        for (Node t : contents) {
            sjBlockContents.add(visualizeBlockNode(t, analysis));
        }
        return sjBlockContents.toString();
    }

    /**
     * Returns the contents of the block.
     *
     * @param bb the block
     * @return the contents of the block, as a list of nodes
     */
    protected List<Node> addBlockContent(Block bb) {
        switch (bb.getType()) {
            case REGULAR_BLOCK:
                return ((RegularBlock) bb).getContents();
            case EXCEPTION_BLOCK:
                return Collections.singletonList(((ExceptionBlock) bb).getNode());
            case CONDITIONAL_BLOCK:
                return Collections.emptyList();
            case SPECIAL_BLOCK:
                return Collections.emptyList();
            default:
                throw new Error("Unrecognized basic block type: " + bb.getType());
        }
    }

    /**
     * Visualize the transfer input of a block.
     *
     * @param bb the block
     * @param analysis the current analysis
     * @param escapeString the escape String for the special need of visualization, e.g., "\\l" for
     *     {@link DOTCFGVisualizer} to keep line left-justification, "\n" for {@link
     *     StringCFGVisualizer} to simply add a new line
     * @return the String representation of the transfer input of the block
     */
    protected String visualizeBlockTransferInputHelper(
            Block bb, Analysis<A, S, T> analysis, String escapeString) {
        assert analysis != null
                : "analysis should be non-null when visualizing the transfer input of a block.";

        TransferInput<A, S> input = analysis.getInput(bb);
        assert input != null;

        StringBuilder sbStore = new StringBuilder();

        // Split input representation to two lines.
        sbStore.append("Before: ");
        if (!input.containsTwoStores()) {
            S regularStore = input.getRegularStore();
            sbStore.append(visualizeStore(regularStore));
        } else {
            S thenStore = input.getThenStore();
            sbStore.append("then=");
            sbStore.append(visualizeStore(thenStore));
            S elseStore = input.getElseStore();
            sbStore.append(", else=");
            sbStore.append(visualizeStore(elseStore));
        }
        sbStore.append(escapeString).append("~~~~~~~~~").append(escapeString);
        return sbStore.toString();
    }

    /**
     * Visualize a special block.
     *
     * @param sbb the special block
     * @param separator the separator String to put at the end of the result
     * @return the String representation of the special block, followed by the separator
     */
    protected String visualizeSpecialBlockHelper(SpecialBlock sbb, String separator) {
        switch (sbb.getSpecialType()) {
            case ENTRY:
                return "<entry>" + separator;
            case EXIT:
                return "<exit>" + separator;
            case EXCEPTIONAL_EXIT:
                return "<exceptional-exit>" + separator;
            default:
                return "";
        }
    }

    /**
     * Returns the last node of a block, or null if none.
     *
     * @param bb the block
     * @return the last node of this block or {@code null}
     */
    protected Node getLastNode(Block bb) {
        switch (bb.getType()) {
            case REGULAR_BLOCK:
                List<Node> blockContents = ((RegularBlock) bb).getContents();
                return blockContents.get(blockContents.size() - 1);
            case EXCEPTION_BLOCK:
                return ((ExceptionBlock) bb).getNode();
            default:
                return null;
        }
    }

    /**
     * Generate the order of processing blocks. Because a block may appears more than once in {@link
     * ControlFlowGraph#getDepthFirstOrderedBlocks()}, the orders of each block are stored in a
     * separate array list.
     *
     * @param cfg the current control flow graph
     * @return an IdentityHashMap that maps from blocks to their orders
     */
    protected IdentityHashMap<Block, List<Integer>> getProcessOrder(ControlFlowGraph cfg) {
        IdentityHashMap<Block, List<Integer>> depthFirstOrder = new IdentityHashMap<>();
        int count = 1;
        for (Block b : cfg.getDepthFirstOrderedBlocks()) {
            depthFirstOrder.computeIfAbsent(b, k -> new ArrayList<>());
            depthFirstOrder.get(b).add(count++);
        }
        return depthFirstOrder;
    }

    @Override
    public String visualizeStore(S store) {
        return store.visualize(this);
    }

    /**
     * Generate the String representation of the nodes of a control flow graph.
     *
     * @param blocks the set of all the blocks in a control flow graph
     * @param cfg the control flow graph
     * @param analysis the current analysis
     * @return the String representation of the nodes
     */
    protected abstract String visualizeNodes(
            Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<A, S, T> analysis);

    /**
     * Generate the String representation of an edge.
     *
     * @param sId the ID of current block
     * @param eId the ID of successor block
     * @param flowRule the content of the edge
     * @return the String representation of the edge
     */
    protected abstract String addEdge(long sId, long eId, String flowRule);

    /**
     * Return the header of the generated graph.
     *
     * @return the String representation of the header of the control flow graph
     */
    protected abstract String visualizeGraphHeader();

    /**
     * Return the footer of the generated graph.
     *
     * @return the String representation of the footer of the control flow graph
     */
    protected abstract String visualizeGraphFooter();

    /**
     * Return the simple String of the process order of a node, e.g., "Process order: 23". When a
     * node have multiple process orders, a sequence of numbers will be returned, e.g., "Process
     * order: 23,25".
     *
     * @param order the list of the process order to be processed
     * @return the String representation of the process order of the node
     */
    protected String getProcessOrderSimpleString(List<Integer> order) {
        return "Process order: " + order.toString().replaceAll("[\\[\\]]", "");
    }

    /**
     * Get the simple name of a node.
     *
     * @param t a node
     * @return the node's simple name, without "Node"
     */
    protected String getNodeSimpleName(Node t) {
        String name = t.getClass().getSimpleName();
        return name.replace("Node", "");
    }
}
