package org.checkerframework.dataflow.cfg;

import com.sun.tools.javac.tree.JCTree;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.Block.BlockType;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.UserError;

/** Generate a graph description in the DOT language of a control graph. */
public class DOTCFGVisualizer<
                A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
        extends AbstractCFGVisualizer<A, S, T> {

    /** Designate the output directory. */
    protected String outDir;

    /** Initialized in {@link #init(Map)}. Use it as a part of the name of the output dot file. */
    protected String checkerName;

    /** Mapping from class/method representation to generated dot file. */
    protected Map<String, String> generated;

    /** Terminate the lines that are left justified. */
    protected final String leftJustified = "\\l";

    @Override
    public void init(Map<String, Object> args) {
        super.init(args);
        this.outDir = (String) args.get("outdir");
        this.checkerName = (String) args.get("checkerName");
        this.generated = new HashMap<>();
    }

    @Override
    public @Nullable Map<String, Object> visualize(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<A, S, T> analysis) {

        String dotGraph = generateGraph(cfg, entry, analysis);
        String dotFileName = dotOutputFileName(cfg.underlyingAST);

        try {
            FileWriter fStream = new FileWriter(dotFileName);
            BufferedWriter out = new BufferedWriter(fStream);
            out.write(dotGraph);
            out.close();
        } catch (IOException e) {
            throw new UserError(
                    "Error creating dot file: " + dotFileName + "; ensure the path is valid", e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("dotFileName", dotFileName);

        return res;
    }

    /**
     * Generate the nodes of control flow graph as String.
     *
     * @param visited the set of all the blocks
     * @param cfg the current control flow graph
     * @param analysis the current analysis
     * @return the String representation of the nodes
     */
    @Override
    public String generateNodes(
            Set<Block> visited, ControlFlowGraph cfg, @Nullable Analysis<A, S, T> analysis) {

        StringBuilder sbDotNodes = new StringBuilder();
        sbDotNodes
                .append("    node [shape=rectangle];")
                .append(lineSeparator)
                .append(lineSeparator);

        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // definition of all nodes including their labels
        for (Block v : visited) {
            sbDotNodes.append("    ").append(v.getId()).append(" [");
            if (v.getType() == BlockType.CONDITIONAL_BLOCK) {
                sbDotNodes.append("shape=polygon sides=8 ");
            } else if (v.getType() == BlockType.SPECIAL_BLOCK) {
                sbDotNodes.append("shape=oval ");
            }
            sbDotNodes.append("label=\"");
            if (verbose) {
                sbDotNodes
                        .append(getProcessOrderSimpleString(processOrder.get(v)))
                        .append(leftJustified);
            }
            sbDotNodes.append(visualizeBlock(v, analysis));
        }

        sbDotNodes.append(lineSeparator);
        return sbDotNodes.toString();
    }

    @Override
    protected String addEdge(long sId, long eId, String flowRule) {
        return "    " + sId + " -> " + eId + " [label=\"" + flowRule + "\"];" + lineSeparator;
    }

    @Override
    public String visualizeBlock(Block bb, @Nullable Analysis<A, S, T> analysis) {
        return super.visualizeBlockHelper(
                bb,
                analysis,
                " \",];" + lineSeparator,
                "?? empty ?? \",];" + lineSeparator,
                leftJustified);
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return super.visualizeSpecialBlockHelper(sbb, "");
    }

    @Override
    public String visualizeBlockTransferInput(Block bb, Analysis<A, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(bb, analysis, leftJustified);
    }

    /**
     * Create the name of dot file.
     *
     * @param ast an abstract syntax tree
     * @return the file name used for DOT output
     */
    protected String dotOutputFileName(UnderlyingAST ast) {
        StringBuilder srcLoc = new StringBuilder();
        StringBuilder outFile = new StringBuilder(outDir);

        outFile.append("/");

        if (ast.getKind() == UnderlyingAST.Kind.ARBITRARY_CODE) {
            CFGStatement cfgStatement = (CFGStatement) ast;
            String clsName = cfgStatement.getClassTree().getSimpleName().toString();
            outFile.append(clsName);
            outFile.append("-initializer-");
            outFile.append(ast.hashCode());

            srcLoc.append("<");
            srcLoc.append(clsName);
            srcLoc.append("::initializer::");
            srcLoc.append(((JCTree) cfgStatement.getCode()).pos);
            srcLoc.append(">");
        } else if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            CFGMethod cfgMethod = (CFGMethod) ast;
            String clsName = cfgMethod.getClassTree().getSimpleName().toString();
            String methodName = cfgMethod.getMethod().getName().toString();
            outFile.append(clsName);
            outFile.append("-");
            outFile.append(methodName);

            srcLoc.append("<");
            srcLoc.append(clsName);
            srcLoc.append("::");
            srcLoc.append(methodName);
            srcLoc.append("(");
            srcLoc.append(cfgMethod.getMethod().getParameters());
            srcLoc.append(")::");
            srcLoc.append(((JCTree) cfgMethod.getMethod()).pos);
            srcLoc.append(">");
        } else {
            throw new BugInCF("Unexpected AST kind: " + ast.getKind() + " value: " + ast);
        }
        outFile.append("-");
        outFile.append(checkerName);
        outFile.append(".dot");

        // make path safe for Windows
        String out = outFile.toString().replace("<", "_").replace(">", "");

        generated.put(srcLoc.toString(), out);

        return out;
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<A, S, T> analysis) {
        StringBuilder sbBlockNode = new StringBuilder();
        sbBlockNode
                .append(escapeDoubleQuotes(t))
                .append("   [ ")
                .append(getNodeSimpleName(t))
                .append(" ]");
        if (analysis != null) {
            A value = analysis.getValue(t);
            if (value != null) {
                sbBlockNode.append("    > ").append(escapeDoubleQuotes(value));
            }
        }
        return sbBlockNode.toString();
    }

    @Override
    public String visualizeStoreThisVal(A value) {
        return storeEntryIndent + "this > " + value + leftJustified;
    }

    @Override
    public String visualizeStoreLocalVar(FlowExpressions.LocalVariable localVar, A value) {
        return storeEntryIndent + localVar + " > " + escapeDoubleQuotes(value) + leftJustified;
    }

    @Override
    public String visualizeStoreFieldVals(FlowExpressions.FieldAccess fieldAccess, A value) {
        return storeEntryIndent + fieldAccess + " > " + escapeDoubleQuotes(value) + leftJustified;
    }

    @Override
    public String visualizeStoreArrayVal(FlowExpressions.ArrayAccess arrayValue, A value) {
        return storeEntryIndent + arrayValue + " > " + escapeDoubleQuotes(value) + leftJustified;
    }

    @Override
    public String visualizeStoreMethodVals(FlowExpressions.MethodCall methodCall, A value) {
        return storeEntryIndent + escapeDoubleQuotes(methodCall) + " > " + value + leftJustified;
    }

    @Override
    public String visualizeStoreClassVals(FlowExpressions.ClassName className, A value) {
        return storeEntryIndent + className + " > " + escapeDoubleQuotes(value) + leftJustified;
    }

    @Override
    public String visualizeStoreKeyVal(String keyName, Object value) {
        return storeEntryIndent + keyName + " = " + value + leftJustified;
    }

    /**
     * Escape the double quotes from the input String. This is for the specification of the dot
     * files.
     *
     * @param str the String to be processed
     * @return the String that has been processed
     */
    private String escapeDoubleQuotes(final String str) {
        return str.replace("\"", "\\\"");
    }

    /**
     * Escape the double quotes from the input {@code Object}.
     *
     * @param obj the input Object
     * @return the String representation of the Object that has been processed
     */
    private String escapeDoubleQuotes(final Object obj) {
        return escapeDoubleQuotes(String.valueOf(obj));
    }

    @Override
    public String visualizeStoreHeader(String classCanonicalName) {
        return classCanonicalName + " (" + leftJustified;
    }

    @Override
    public String visualizeStoreFooter() {
        return ")";
    }

    /**
     * Write a file {@code methods.txt} that contains a mapping from source code location to
     * generated dot file.
     */
    @Override
    public void shutdown() {
        try {
            // Open for append, in case of multiple sub-checkers.
            FileWriter fstream = new FileWriter(outDir + "/methods.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            for (Map.Entry<String, String> kv : generated.entrySet()) {
                out.write(kv.getKey());
                out.append("\t");
                out.write(kv.getValue());
                out.append(lineSeparator);
            }
            out.close();
        } catch (IOException e) {
            throw new UserError(
                    "Error creating methods.txt file in: " + outDir + "; ensure the path is valid",
                    e);
        }
    }

    @Override
    protected String visualizeGraphHeader() {
        return "digraph {" + lineSeparator;
    }

    @Override
    protected String visualizeGraphFooter() {
        return "}" + lineSeparator;
    }
}
