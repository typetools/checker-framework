package org.checkerframework.dataflow.cfg;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.Block.BlockType;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.ErrorReporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.tree.JCTree;

/**
 * Generate a graph description in the DOT language of a control graph.
 *
 * @author Stefan Heule
 *
 */
public class DOTCFGVisualizer<A extends AbstractValue<A>,
        S extends Store<S>, T extends TransferFunction<A, S>>
        implements CFGVisualizer<A, S, T> {

    protected String outdir;
    protected boolean verbose;
    protected String checkerName;
    //sbStore is for visualizing store
    protected StringBuilder sbStore;
    // Mapping from class/method representation to generated dot file.
    protected Map<String, String> generated;

    public void init(Map<String, Object> args) {
        this.outdir = (String) args.get("outdir");
        {
            Object verb = args.get("verbose");
            this.verbose = verb == null ? false :
                verb instanceof String ? Boolean.getBoolean((String) verb) :
                    (boolean) verb;
        }
        this.checkerName = (String) args.get("checkerName");

        this.generated = new HashMap<>();

        this.sbStore = new StringBuilder();
    }

    /**
     * {@inheritDoc}
     */
    public /*@Nullable*/ Map<String, Object> visualize(ControlFlowGraph cfg, Block entry,
            /*@Nullable*/ Analysis<A, S, T> analysis) {

        String dotgraph = generateDotGraph(cfg, entry, analysis);

        String dotfilename = dotOutputFileName(cfg.underlyingAST);
        // System.err.println("Output to DOT file: " + dotfilename);

        try {
            FileWriter fstream = new FileWriter(dotfilename);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(dotgraph);
            out.close();
        } catch (IOException e) {
            ErrorReporter.errorAbort("Error creating dot file: " + dotfilename +
                    "; ensure the path is valid", e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("dotFileName", dotfilename);

        return res;
    }

    /**
     * generate the dot string write to dot file
     * @param cfg
     * @param entry
     * @param analysis
     * @return
     */
    protected String generateDotGraph(ControlFlowGraph cfg, Block entry,
        /*@Nullable*/ Analysis<A, S, T> analysis) {
        StringBuilder sbDigraph = new StringBuilder();
        Set<Block> visited = new HashSet<>();

        // header
        sbDigraph.append("digraph {\n");

        /*generateDotEdges must called BEFORE generateDotNodes,
         * since DoteNodes needs the information in 'Set<Block> visited' that created in generateDotEdges()  */
        generateDotEdges(sbDigraph, visited, entry);

        generateDotNodes(sbDigraph, visited, cfg, analysis);

        // footer
        sbDigraph.append("}\n");

        return sbDigraph.toString();
    }

    private void generateDotNodes(StringBuilder sbDigraph, Set<Block> visited,
    ControlFlowGraph cfg, Analysis<A, S, T> analysis) {
        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        sbDigraph.append("    node [shape=rectangle];\n\n");
        // definition of all nodes including their labels
        for (Block v : visited) {
            sbDigraph.append("    " + v.getId() + " [");
            if (v.getType() == BlockType.CONDITIONAL_BLOCK) {
                sbDigraph.append("shape=polygon sides=8 ");
            } else if (v.getType() == BlockType.SPECIAL_BLOCK) {
                sbDigraph.append("shape=oval ");
            }
            sbDigraph.append("label=\"");
            if (verbose) {
                sbDigraph.append("Process order: " + processOrder.get(v).toString().replaceAll("[\\[\\]]", "") + "\\n");
            }
            sbDigraph.append(visualizeBlock(v, analysis).replace("\\n", "\\l")
            + " \",];\n");
        }

        sbDigraph.append("\n");
	}

	protected void generateDotEdges(StringBuilder sbDigraph, Set<Block> visited, Block entry) {
        Block cur = entry;
        Queue<Block> worklist = new LinkedList<>();
        visited.add(entry);
        // traverse control flow graph and define all arrows
        while (true) {
            if (cur == null)
                break;

            if (cur.getType() == BlockType.CONDITIONAL_BLOCK) {
                ConditionalBlock ccur = ((ConditionalBlock) cur);
                Block thenSuccessor = ccur.getThenSuccessor();
                addDotEdge(sbDigraph, ccur.getId(), thenSuccessor.getId(), "then\\n" + ccur.getThenFlowRule());
                if (!visited.contains(thenSuccessor)) {
                    visited.add(thenSuccessor);
                    worklist.add(thenSuccessor);
                }
                Block elseSuccessor = ccur.getElseSuccessor();
                addDotEdge(sbDigraph, ccur.getId(), elseSuccessor.getId(), "else\\n" + ccur.getElseFlowRule());
                if (!visited.contains(elseSuccessor)) {
                    visited.add(elseSuccessor);
                    worklist.add(elseSuccessor);
                }
            } else {
                assert cur instanceof SingleSuccessorBlock;
                Block b = ((SingleSuccessorBlock) cur).getSuccessor();
                if (b != null) {
                    addDotEdge(sbDigraph, cur.getId(), b.getId(), ((SingleSuccessorBlock) cur).getFlowRule().name());
                    if (!visited.contains(b)) {
                        visited.add(b);
                        worklist.add(b);
                    }
                }
            }

            // exceptional edges
            if (cur.getType() == BlockType.EXCEPTION_BLOCK) {
                ExceptionBlock ecur = (ExceptionBlock) cur;
                for (Entry<TypeMirror, Set<Block>> e : ecur
                        .getExceptionalSuccessors().entrySet()) {
                    Set<Block> blocks = e.getValue();
                    TypeMirror cause = e.getKey();
                    String exception = cause.toString();
                    if (exception.startsWith("java.lang.")) {
                        exception = exception.replace("java.lang.", "");
                    }

                    for (Block b : blocks) {
                        addDotEdge(sbDigraph, cur.getId(), b.getId(), exception);
                        if (!visited.contains(b)) {
                            visited.add(b);
                            worklist.add(b);
                        }
                    }
                }
            }

            cur = worklist.poll();
        }
    }

    /** @return The file name used for DOT output. */
    protected String dotOutputFileName(UnderlyingAST ast) {
        StringBuilder srcloc = new StringBuilder();

        StringBuilder outfile = new StringBuilder(outdir);
        outfile.append('/');
        if (ast.getKind() == UnderlyingAST.Kind.ARBITRARY_CODE) {
            CFGStatement cfgs = (CFGStatement) ast;
            String clsname = cfgs.getClassTree().getSimpleName().toString();
            outfile.append(clsname);
            outfile.append("-initializer-");
            outfile.append(ast.hashCode());

            srcloc.append('<');
            srcloc.append(clsname);
            srcloc.append("::initializer::");
            srcloc.append(((JCTree)cfgs.getCode()).pos);
            srcloc.append('>');
        } else if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            CFGMethod cfgm = (CFGMethod) ast;
            String clsname = cfgm.getClassTree().getSimpleName().toString();
            String methname = cfgm.getMethod().getName().toString();
            outfile.append(clsname);
            outfile.append('-');
            outfile.append(methname);

            srcloc.append('<');
            srcloc.append(clsname);
            srcloc.append("::");
            srcloc.append(methname);
            srcloc.append('(');
            srcloc.append(cfgm.getMethod().getParameters());
            srcloc.append(")::");
            srcloc.append(((JCTree)cfgm.getMethod()).pos);
            srcloc.append('>');
        } else {
            ErrorReporter.errorAbort("Unexpected AST kind: " + ast.getKind() +
                " value: " + ast.toString());
            return null;
        }
        outfile.append('-');
        outfile.append(checkerName);
        outfile.append(".dot");

        // make path safe for Windows
        String out = outfile.toString().replace("<", "_").replace(">", "");

        generated.put(srcloc.toString(), out);

        return out;
    }

    protected IdentityHashMap<Block, List<Integer>> getProcessOrder(ControlFlowGraph cfg) {
        IdentityHashMap<Block, List<Integer>> depthFirstOrder = new IdentityHashMap<>();
        int count = 1;
        for (Block b : cfg.getDepthFirstOrderedBlocks()) {
            if (depthFirstOrder.get(b) == null) {
                depthFirstOrder.put(b, new ArrayList<Integer>());
            }
            depthFirstOrder.get(b).add(count++);
        }
        return depthFirstOrder;
    }

    /**
     * Produce a string representation of the contests of a basic block.
     *
     * @param bb
     *            Basic block to visualize.
     * @return String representation.
     */
    protected String visualizeBlock(
            Block bb,
            /*@Nullable*/ Analysis<A, S, T> analysis) {

        StringBuilder sbBlock = new StringBuilder();

        // loop over contents
        List<Node> contents = new LinkedList<>();
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
        boolean notFirst = false;
        for (Node t : contents) {
            if (notFirst) {
                sbBlock.append("\\n");
            }
            notFirst = true;
            sbBlock.append(visualizeBlockNode(t, analysis));
        }

        // handle case where no contents are present
        boolean centered = false;
        if (sbBlock.length() == 0) {
            centered = true;
            if (bb.getType() == BlockType.SPECIAL_BLOCK) {
                visualizeSpecialBlock((SpecialBlock) bb, sbBlock);
            } else if (bb.getType() == BlockType.CONDITIONAL_BLOCK) {
                return "";
            } else {
                return "?? empty ??";
            }
        }

        // visualize transfer input if necessary
        if (analysis != null) {
            visualizeBlockTransferInput(bb, analysis, sbBlock);
        }

        return sbBlock.toString() + (centered ? "" : "\\n");
    }

    protected void visualizeSpecialBlock(SpecialBlock sbb, StringBuilder sbBlock) {
        switch (sbb.getSpecialType()) {
        case ENTRY:
            sbBlock.append("<entry>");
            break;
        case EXIT:
            sbBlock.append("<exit>");
            break;
        case EXCEPTIONAL_EXIT:
            sbBlock.append("<exceptional-exit>");
            break;
        }
    }

    protected void visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis,
    StringBuilder sbBlockContent) {
        TransferInput<A, S> input = analysis.getInput(bb);
        this.sbStore.setLength(0);

        // split input representation to two lines
        this.sbStore.append("Before:");
        S thenStore = input.getThenStore();
        if (thenStore == null) {
            S regularStore = input.getRegularStore();
            this.sbStore.append('[');
            visualizeStore(regularStore);
            this.sbStore.append(']');
        } else {
            S elseStore = input.getElseStore();
            this.sbStore.append("[then=");
            visualizeStore(thenStore);
            this.sbStore.append(", else=");
            visualizeStore(elseStore);
            this.sbStore.append("]");
        }
        // separator
        this.sbStore.append("\\n~~~~~~~~~\\n");

        //the transfer input before this block should add before this block content
        sbBlockContent.insert(0, this.sbStore);

        if (verbose) {
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
            if (lastNode != null) {
                this.sbStore.setLength(0);
                this.sbStore.append("\\n~~~~~~~~~\\n");
                this.sbStore.append("After:");
                visualizeStore(analysis.getResult().getStoreAfter(lastNode.getTree()));
                sbBlockContent.append(this.sbStore);
            }
        }
    }

	protected String visualizeBlockNode(Node t, /*@Nullable*/ Analysis<A, S, T> analysis) {
        A value = analysis.getValue(t);
        String valueInfo = "";
        if (value != null) {
            valueInfo = "    > " + prepareString(value.toString());
        }
        return prepareString(t.toString()) + "   [ " + visualizeType(t) + " ]" + valueInfo;
    }

    protected String visualizeType(Node t) {
        String name = t.getClass().getSimpleName();
        return name.replace("Node", "");
    }

    protected String prepareString(String s) {
        return s.replace("\"", "\\\"");
    }

    /**
     * @param sId
     * @param eId
     * @param labelContent
     * @return
     */
    protected void addDotEdge(StringBuilder sbDigraph, long sId, long eId, String labelContent) {
    	sbDigraph.append("    " + sId + " -> "+ eId + " [label=\""+ labelContent + "\"];\n");
    }

    /** Write a file methods.txt that contains a mapping from
     * source code location to generated dot file.
     */
    public void shutdown() {
        try {
            // Open for append, in case of multiple sub-checkers.
            FileWriter fstream = new FileWriter(outdir + "/methods.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            for (Map.Entry<String, String> kv : generated.entrySet()) {
                out.write(kv.getKey());
                out.append('\t');
                out.write(kv.getValue());
                out.append('\n');
            }
            out.close();
        } catch (IOException e) {
            ErrorReporter.errorAbort("Error creating methods.txt file in: " + outdir +
                    "; ensure the path is valid", e);
        }
    }

	@Override
	public void visualizeStore(S store) {
		store.visualize(this);
	}

	@Override
	public void visualizeThisValue(A value) {
		this.sbStore.append("  this > " + value
                + "\\n");
	}

	@Override
	public void visualizeLocalVariable(FlowExpressions.LocalVariable localVar, A value) {
        this.sbStore.append("  " + localVar + " > " +
            toStringEscapeDoubleQuotes(value)
            + "\\n");
	}

	@Override
	public void visualizeFieldValues(FlowExpressions.FieldAccess fieldAccess, A value) {
		this.sbStore.append("  " + fieldAccess + " > " +
            toStringEscapeDoubleQuotes(value)
            + "\\n");
	}

	@Override
	public void visualizeArrayValue(FlowExpressions.ArrayAccess arrayValue, A value) {
		this.sbStore.append("  " + arrayValue + " > " +
            toStringEscapeDoubleQuotes(value)
            + "\\n");
    }

	@Override
	public void visualizeMethodValues(FlowExpressions.PureMethodCall methodCall, A value) {
		this.sbStore.append("  " + methodCall.toString().replace("\"", "\\\"")
                + " > " + value + "\\n");
	}

	@Override
	public void visualizeClassValues(FlowExpressions.ClassName className, A value) {
		this.sbStore.append("  " + className + " > " + toStringEscapeDoubleQuotes(value)
                + "\\n");
	}

	@Override
	public void visualizeKeyValue(String keyName, Object value) {
		this.sbStore.append("  "+keyName+" = "+value+"\\n");
	}

	protected String escapeDoubleQuotes(final String str) {
        return str.replace("\"", "\\\"");
    }


    protected String toStringEscapeDoubleQuotes(final Object obj) {
        return escapeDoubleQuotes(String.valueOf(obj));
    }

	@Override
	public void visualizeStoreHeader(String classCanonicalName) {
		this.sbStore.append(classCanonicalName + " (\\n");
	}

	@Override
	public void visualizeStoreFooter() {
		this.sbStore.append(")");
	}

}
