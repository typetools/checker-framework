package org.checkerframework.dataflow.cfg;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
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

import javax.lang.model.type.TypeMirror;

import com.sun.tools.javac.tree.JCTree;

/**
 * Generate a graph description in the DOT language of a control graph.
 *
 * @author Stefan Heule
 *
 */
public class DOTCFGVisualizer implements CFGVisualizer {

    protected String outdir;
    protected boolean verbose;
    protected String checkerName;

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
    }

    /**
     * {@inheritDoc}
     */
    public <A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
    /*@Nullable*/ Map<String, Object> visualize(ControlFlowGraph cfg, Block entry,
            /*@Nullable*/ Analysis<A, S, T> analysis) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        Set<Block> visited = new HashSet<>();
        Queue<Block> worklist = new LinkedList<>();
        Block cur = entry;
        visited.add(entry);

        // header
        sb1.append("digraph {\n");
        sb1.append("    node [shape=rectangle];\n\n");

        // traverse control flow graph and define all arrows
        while (true) {
            if (cur == null)
                break;

            if (cur.getType() == BlockType.CONDITIONAL_BLOCK) {
                ConditionalBlock ccur = ((ConditionalBlock) cur);
                Block thenSuccessor = ccur.getThenSuccessor();
                sb2.append("    " + ccur.getId() + " -> "
                        + thenSuccessor.getId());
                sb2.append(" [label=\"then\\n" + ccur.getThenFlowRule() + "\"];\n");
                if (!visited.contains(thenSuccessor)) {
                    visited.add(thenSuccessor);
                    worklist.add(thenSuccessor);
                }
                Block elseSuccessor = ccur.getElseSuccessor();
                sb2.append("    " + ccur.getId() + " -> "
                        + elseSuccessor.getId());
                sb2.append(" [label=\"else\\n" + ccur.getElseFlowRule() + "\"];\n");
                if (!visited.contains(elseSuccessor)) {
                    visited.add(elseSuccessor);
                    worklist.add(elseSuccessor);
                }
            } else {
                assert cur instanceof SingleSuccessorBlock;
                Block b = ((SingleSuccessorBlock) cur).getSuccessor();
                if (b != null) {
                    sb2.append("    " + cur.getId() + " -> " + b.getId());
                    sb2.append(" [label=\"" + ((SingleSuccessorBlock) cur).getFlowRule() + "\"];\n");
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
                        sb2.append("    " + cur.getId() + " -> " + b.getId());
                        sb2.append(" [label=\"" + exception + "\"];\n");
                        if (!visited.contains(b)) {
                            visited.add(b);
                            worklist.add(b);
                        }
                    }
                }
            }

            cur = worklist.poll();
        }

        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // definition of all nodes including their labels
        for (Block v : visited) {
            sb1.append("    " + v.getId() + " [");
            if (v.getType() == BlockType.CONDITIONAL_BLOCK) {
                sb1.append("shape=polygon sides=8 ");
            } else if (v.getType() == BlockType.SPECIAL_BLOCK) {
                sb1.append("shape=oval ");
            }
            sb1.append("label=\"");
            if (verbose) {
                sb1.append("Process order: " + processOrder.get(v).toString().replaceAll("[\\[\\]]", "") + "\\n");
            }
            sb1.append(visualizeContent(v, analysis, verbose).replace("\\n", "\\l")
                    + " \",];\n");
        }

        sb1.append("\n");
        sb1.append(sb2);

        // footer
        sb1.append("}\n");

        String dotstring = sb1.toString();

        String dotfilename = dotOutputFileName(cfg.underlyingAST);
        // System.err.println("Output to DOT file: " + dotfilename);

        try {
            FileWriter fstream = new FileWriter(dotfilename);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(dotstring);
            out.close();
        } catch (IOException e) {
            ErrorReporter.errorAbort("Error creating dot file: " + dotfilename +
                    "; ensure the path is valid", e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("dotFileName", dotfilename);
        return res;
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
    protected <A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>> String visualizeContent(
            Block bb,
            /*@Nullable*/ Analysis<A, S, T> analysis,
            boolean verbose) {

        StringBuilder sb = new StringBuilder();

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
                sb.append("\\n");
            }
            notFirst = true;
            sb.append(visualizeNode(t, analysis));
        }

        // handle case where no contents are present
        boolean centered = false;
        if (sb.length() == 0) {
            centered = true;
            if (bb.getType() == BlockType.SPECIAL_BLOCK) {
                SpecialBlock sbb = (SpecialBlock) bb;
                switch (sbb.getSpecialType()) {
                case ENTRY:
                    sb.append("<entry>");
                    break;
                case EXIT:
                    sb.append("<exit>");
                    break;
                case EXCEPTIONAL_EXIT:
                    sb.append("<exceptional-exit>");
                    break;
                }
            } else if (bb.getType() == BlockType.CONDITIONAL_BLOCK) {
                return "";
            } else {
                return "?? empty ??";
            }
        }

        // visualize transfer input if necessary
        if (analysis != null) {
            TransferInput<A, S> input = analysis.getInput(bb);
            StringBuilder sb2 = new StringBuilder();

            // split input representation to two lines
            String s = input.toDOToutput().replace("}, else={", "}\\nelse={");
            sb2.append("Before:");
            sb2.append(s.subSequence(1, s.length() - 1));

            // separator
            sb2.append("\\n~~~~~~~~~\\n");
            sb2.append(sb);
            sb = sb2;

            if (verbose) {
                Node lastNode;
                switch (bb.getType()) {
                    case REGULAR_BLOCK:
                        List<Node> blockContents = ((RegularBlock) bb).getContents();
                        lastNode = contents.get(blockContents.size() - 1);
                        break;
                    case EXCEPTION_BLOCK:
                        lastNode = ((ExceptionBlock) bb).getNode();
                        break;
                    default:
                        lastNode = null;
                }
                if (lastNode != null) {
                    sb2.append("\\n~~~~~~~~~\\n");
                    s = analysis.getResult().getStoreAfter(lastNode.getTree()).
                            toDOToutput().replace("}, else={", "}\\nelse={");
                    sb2.append("After:");
                    sb2.append(s);
                }
            }
        }

        return sb.toString() + (centered ? "" : "\\n");
    }

    protected <A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
    String visualizeNode(Node t, /*@Nullable*/ Analysis<A, S, T> analysis) {
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

}
