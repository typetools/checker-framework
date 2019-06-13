package org.checkerframework.dataflow.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
 * necessary. Some of the simpler methods in {@link CFGVisualizer} need to be implemented by
 * subclasses. Additional abstract methods make customizations easy.
 *
 * <p>This class also provides some helper methods to make building custom CFGVisualizers easier.
 *
 * @see DOTCFGVisualizer
 * @see StringCFGVisualizer
 */
public abstract class AbstractCFGVisualizer<
                A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
        implements CFGVisualizer<A, S, T> {

    /**
     * Initialized in {@link #init(Map)}. If its value is {@code true}, {@link CFGVisualizer} will
     * return more detailed information.
     */
    protected boolean verbose;

    /** The line separator. */
    protected final String lineSeparator = System.lineSeparator();

    /**
     * Indicate 2 white space characters as the indentation to the elements of the {@link Store}.
     */
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
     * Generate the control flow graph.
     *
     * @param cfg the current control flow graph
     * @param entry the entry block of the control flow graph
     * @param analysis the current analysis
     * @return the specific representation of the control flow graph, e.g., DOT, String
     */
    protected String generateGraph(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {
        return visualizeGraphHeader()
                + generateGraphHelper(cfg, entry, analysis)
                + visualizeGraphFooter();
    }

    /**
     * Helper method to simplify generating a control flow graph.
     *
     * @param cfg the control flow graph
     * @param entry the entry {@link Block}
     * @param analysis the current analysis
     * @return the String representation of the control flow graph
     */
    protected String generateGraphHelper(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {
        Set<Block> visited = new HashSet<>();
        StringBuilder sbDigraph = new StringBuilder();
        Queue<Block> workList = new ArrayDeque<>();
        Block cur = entry;
        visited.add(entry);
        while (cur != null) {
            handleSuccessorsHelper(cur, visited, workList, sbDigraph);
            cur = workList.poll();
        }
        sbDigraph.append(generateNodes(visited, cfg, analysis));
        return sbDigraph.toString();
    }

    /**
     * Helper method called by {@link #generateGraphHelper(ControlFlowGraph, Block, Analysis)}. It
     * checks the successors of the {@link Block}s, and, if possible, adds all the successors to the
     * work list and the visited {@link Block}s list.
     *
     * @param cur the current {@link Block}
     * @param visited the set of {@link Block}s that have already been visited
     * @param workList the queue of {@link Block}s to be processed
     * @param sbDigraph the {@link StringBuilder} to store the graph
     */
    protected void handleSuccessorsHelper(
            Block cur, Set<Block> visited, Queue<Block> workList, StringBuilder sbDigraph) {
        if (cur.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
            ConditionalBlock ccur = ((ConditionalBlock) cur);
            Block thenSuccessor = ccur.getThenSuccessor();
            sbDigraph.append(
                    addEdge(
                            ccur.getId(),
                            thenSuccessor.getId(),
                            ccur.getThenFlowRule().toString()));
            addBlock(thenSuccessor, visited, workList);
            Block elseSuccessor = ccur.getElseSuccessor();
            sbDigraph.append(
                    addEdge(
                            ccur.getId(),
                            elseSuccessor.getId(),
                            ccur.getElseFlowRule().toString()));
            addBlock(elseSuccessor, visited, workList);
        } else {
            assert cur instanceof SingleSuccessorBlock;
            Block b = ((SingleSuccessorBlock) cur).getSuccessor();
            if (b != null) {
                sbDigraph.append(
                        addEdge(
                                cur.getId(),
                                b.getId(),
                                ((SingleSuccessorBlock) cur).getFlowRule().name()));
                addBlock(b, visited, workList);
            }
        }
        if (cur.getType() == Block.BlockType.EXCEPTION_BLOCK) {
            ExceptionBlock ecur = (ExceptionBlock) cur;
            for (Map.Entry<TypeMirror, Set<Block>> e : ecur.getExceptionalSuccessors().entrySet()) {
                Set<Block> blocks = e.getValue();
                TypeMirror cause = e.getKey();
                String exception = cause.toString();
                if (exception.startsWith("java.lang.")) {
                    exception = exception.replace("java.lang.", "");
                }
                for (Block b : blocks) {
                    sbDigraph.append(addEdge(cur.getId(), b.getId(), exception));
                    addBlock(b, visited, workList);
                }
            }
        }
    }

    /**
     * Helper method called by {@link #handleSuccessorsHelper(Block, Set, Queue, StringBuilder)}.
     * Checks whether a block exists in the visited {@link Block}s list, and, if not, adds it to the
     * visited {@link Block}s list and the work list.
     *
     * @param b the {@link Block} to check
     * @param visited the set of {@link Block}s that have already been visited
     * @param workList the queue of {@link Block}s to be processed
     */
    protected void addBlock(Block b, Set<Block> visited, Queue<Block> workList) {
        if (!visited.contains(b)) {
            visited.add(b);
            workList.add(b);
        }
    }

    /**
     * Helper method to simplify visualizing a {@link Block}.
     *
     * @param bb the {@link Block}
     * @param analysis the current analysis
     * @param cbFooter footer for conditional {@link Block}
     * @param osFooter footer for the other situations
     * @param escapeCharacter the escape String to use
     * @return the String representation of the {@link Block}
     */
    protected String visualizeBlockHelper(
            Block bb,
            @Nullable Analysis<A, S, T> analysis,
            String cbFooter,
            String osFooter,
            String escapeCharacter) {
        StringBuilder sbBlock = new StringBuilder();
        sbBlock.append(loopOverBlockContents(bb, analysis, escapeCharacter));

        // Handle case where no contents are present
        boolean notCentered = true;
        if (sbBlock.length() == 0) {
            notCentered = false;
            if (bb.getType() == Block.BlockType.SPECIAL_BLOCK) {
                sbBlock.append(visualizeSpecialBlock((SpecialBlock) bb));
            } else if (bb.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
                sbBlock.append(cbFooter);
                return sbBlock.toString();
            } else {
                sbBlock.append(osFooter);
                return sbBlock.toString();
            }
        }

        // Visualize transfer input if necessary
        if (analysis != null) {
            // The transfer input before this block is added before the block content
            sbBlock.insert(0, visualizeBlockTransferInput(bb, analysis));
            if (verbose) {
                Node lastNode = getLastNode(bb);
                if (lastNode != null) {
                    StringBuilder sbStore = new StringBuilder();
                    sbStore.append(escapeCharacter).append("~~~~~~~~~").append(escapeCharacter);
                    sbStore.append("After: ");
                    sbStore.append(visualizeStore(analysis.getResult().getStoreAfter(lastNode)));
                    sbBlock.append(sbStore);
                }
            }
        }
        if (notCentered) {
            sbBlock.append(escapeCharacter);
        }
        sbBlock.append(cbFooter);
        return sbBlock.toString();
    }

    /**
     * Helper method called by {@link #visualizeBlockHelper}. Iterates over the block content and
     * visualizes all the {@link Node}s in it.
     *
     * @param bb the {@link Block}
     * @param analysis the current analysis
     * @param separator the separator String to use
     * @return the String representation of the contents of the {@link Block}
     */
    protected String loopOverBlockContents(
            Block bb, @Nullable Analysis<A, S, T> analysis, String separator) {

        StringBuilder sbBlockContents = new StringBuilder();
        boolean notFirst = false;

        List<Node> contents = addBlockContent(bb);

        for (Node t : contents) {
            if (notFirst) {
                sbBlockContents.append(separator);
            }
            notFirst = true;
            sbBlockContents.append(visualizeBlockNode(t, analysis));
        }
        return sbBlockContents.toString();
    }

    /**
     * Helper method called by {@link #loopOverBlockContents}. If possible, get a sequence of {@link
     * Node}s for further processing.
     *
     * @param bb the {@link Block}
     * @return a list of {@link Node}s
     */
    protected List<Node> addBlockContent(Block bb) {
        List<Node> contents = new ArrayList<>();
        switch (bb.getType()) {
            case REGULAR_BLOCK:
                contents.addAll(((RegularBlock) bb).getContents());
                break;
            case EXCEPTION_BLOCK:
                contents.add(((ExceptionBlock) bb).getNode());
                break;
            case CONDITIONAL_BLOCK:
                break;
            case SPECIAL_BLOCK:
                break;
            default:
                assert false : "All types of basic blocks covered";
        }
        return contents;
    }

    /**
     * Helper method to simplify visualizing the transfer input of a Block; it is useful when
     * implementing a custom CFGVisualizer.
     *
     * @param bb the {@link Block}
     * @param analysis the current analysis
     * @param escapeCharacter the escape String to use
     * @return the String representation of the transfer input of a {@link Block}
     */
    protected String visualizeBlockTransferInputHelper(
            Block bb, Analysis<A, S, T> analysis, String escapeCharacter) {
        assert analysis != null
                : "analysis should be non-null when visualizing the transfer input of a block.";

        TransferInput<A, S> input = analysis.getInput(bb);
        assert input != null;

        StringBuilder sbStore = new StringBuilder();

        // split input representation to two lines
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
        sbStore.append(escapeCharacter).append("~~~~~~~~~").append(escapeCharacter);
        return sbStore.toString();
    }

    /**
     * Helper method to simplify visualizing a special Block; it is useful when implementing a
     * custom CFGVisualizer.
     *
     * @param sbb the special {@link Block}
     * @param separator the separator String to use
     * @return the String representation of the special {@link Block}
     */
    protected String visualizeSpecialBlockHelper(SpecialBlock sbb, String separator) {
        String specialBlock = "";
        switch (sbb.getSpecialType()) {
            case ENTRY:
                specialBlock = "<entry>" + separator;
                break;
            case EXIT:
                specialBlock = "<exit>" + separator;
                break;
            case EXCEPTIONAL_EXIT:
                specialBlock = "<exceptional-exit>" + separator;
                break;
        }
        return specialBlock;
    }

    /**
     * Helper method called by {@link #visualizeBlockHelper}. If possible, get the last {@link Node}
     * of a {@link Block}.
     *
     * @param bb the {@link Block}
     * @return the last {@link Node} or {@code null}
     */
    protected Node getLastNode(Block bb) {
        Node lastNode;
        switch (bb.getType()) {
            case REGULAR_BLOCK:
                List<Node> blockContents = ((RegularBlock) bb).getContents();
                lastNode = blockContents.get(blockContents.size() - 1);
                break;
            case EXCEPTION_BLOCK:
                lastNode = ((ExceptionBlock) bb).getNode();
                break;
            default:
                lastNode = null;
        }
        return lastNode;
    }

    /**
     * Generate the order of processing {@link Block}s.
     *
     * @param cfg the current control flow graph
     * @return the IdentityHashMap which maps from {@link Block}s to their orders
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
     * <p>This abstract method needs to be implemented to customize the output.
     *
     * @param visited the set of the visited {@link Block}s
     * @param cfg the control flow graph
     * @param analysis the current analysis
     * @return the String representation of the {@link Node}s
     */
    protected abstract String generateNodes(
            Set<Block> visited, ControlFlowGraph cfg, @Nullable Analysis<A, S, T> analysis);

    /**
     * Generate the String representation of an edge.
     *
     * <p>This abstract method needs to be implemented to customize the output.
     *
     * @param sId the ID of current {@link Block}
     * @param eId the ID of successor {@link Block}
     * @param flowRule the content of the edge
     * @return the String representation of the edge
     */
    protected abstract String addEdge(long sId, long eId, String flowRule);

    /**
     * Return the header of the generated graph. Called by {@link
     * #generateGraphHelper(ControlFlowGraph, Block, Analysis)}.
     *
     * <p>This abstract method needs to be implemented to customize the output.
     *
     * @return the String representation of the header of the control flow graph
     */
    protected abstract String visualizeGraphHeader();

    /**
     * Return the footer of the generated graph. Called by {@link
     * #generateGraphHelper(ControlFlowGraph, Block, Analysis)}.
     *
     * <p>This abstract method needs to be implemented to customize the output.
     *
     * @return the String representation of the footer of the control flow graph
     */
    protected abstract String visualizeGraphFooter();

    /**
     * Return the simple String of the process order of a {@link Node}. Called by {@link
     * #generateNodes(Set, ControlFlowGraph, Analysis)}.
     *
     * @param order the list of the process order to be processed.
     * @return the String representation of the process order of a {@link Node}
     */
    protected String getProcessOrderSimpleString(List<Integer> order) {
        return "Process order: " + order.toString().replaceAll("[\\[\\]]", "");
    }

    /**
     * Remove the String "Node" from the name of the {@link Node}.
     *
     * @param t {@link Node}
     * @return the String representation of the {@link Node}'s simple name
     */
    protected String getNodeSimpleName(Node t) {
        String name = t.getClass().getSimpleName();
        return name.replace("Node", "");
    }
}
