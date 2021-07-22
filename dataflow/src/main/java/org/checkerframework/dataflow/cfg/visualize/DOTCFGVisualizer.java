package org.checkerframework.dataflow.cfg.visualize;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGLambda;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGStatement;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.Block.BlockType;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.visualize.AbstractCFGVisualizer.VisualizeWhere;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/** Generate a graph description in the DOT language of a control graph. */
public class DOTCFGVisualizer<
                V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
        extends AbstractCFGVisualizer<V, S, T> {

    /** The output directory. */
    @SuppressWarnings("nullness:initialization.field.uninitialized") // uses init method
    protected String outDir;

    /** The (optional) checker name. Used as a part of the name of the output dot file. */
    protected @Nullable String checkerName;

    /** Mapping from class/method representation to generated dot file. */
    @SuppressWarnings("nullness:initialization.field.uninitialized") // uses init method
    protected Map<String, String> generated;

    /** Terminator for lines that are left-justified. */
    protected static final String leftJustifiedTerminator = "\\l";

    @Override
    @SuppressWarnings("nullness") // assume arguments are set correctly
    public void init(Map<String, Object> args) {
        super.init(args);
        this.outDir = (String) args.get("outdir");
        if (this.outDir == null) {
            throw new BugInCF(
                    "outDir should never be null,"
                            + " provide it in args when calling DOTCFGVisualizer.init(args).");
        }
        this.checkerName = (String) args.get("checkerName");
        this.generated = new HashMap<>();
    }

    @Override
    public String getSeparator() {
        return leftJustifiedTerminator;
    }

    @Override
    public @Nullable Map<String, Object> visualize(
            ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {

        String dotGraph = visualizeGraph(cfg, entry, analysis);
        String dotFileName = dotOutputFileName(cfg.underlyingAST);

        try {
            FileWriter fStream = new FileWriter(dotFileName);
            BufferedWriter out = new BufferedWriter(fStream);
            out.write(dotGraph);
            out.close();
        } catch (IOException e) {
            throw new UserError("Error creating dot file (is the path valid?): " + dotFileName, e);
        }

        return Collections.singletonMap("dotFileName", dotFileName);
    }

    @SuppressWarnings("keyfor:enhancedfor.type.incompatible")
    @Override
    public String visualizeNodes(
            Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<V, S, T> analysis) {

        StringBuilder sbDotNodes = new StringBuilder();

        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // Definition of all nodes including their labels.
        for (@KeyFor("processOrder") Block v : blocks) {
            sbDotNodes.append("    ").append(v.getUid()).append(" [");
            if (v.getType() == BlockType.CONDITIONAL_BLOCK) {
                sbDotNodes.append("shape=polygon sides=8 ");
            } else if (v.getType() == BlockType.SPECIAL_BLOCK) {
                sbDotNodes.append("shape=oval ");
            } else {
                sbDotNodes.append("shape=rectangle ");
            }
            sbDotNodes.append("label=\"");
            if (verbose) {
                sbDotNodes
                        .append(getProcessOrderSimpleString(processOrder.get(v)))
                        .append(getSeparator());
            }
            String strBlock = visualizeBlock(v, analysis);
            if (strBlock.length() == 0) {
                if (v.getType() == BlockType.CONDITIONAL_BLOCK) {
                    // The footer of the conditional block.
                    sbDotNodes.append("\"];");
                } else {
                    // The footer of the block which has no content and is not a special or
                    // conditional block.
                    sbDotNodes.append("?? empty ??\"];");
                }
            } else {
                sbDotNodes.append(strBlock).append("\"];");
            }
            sbDotNodes.append(System.lineSeparator());
        }
        return sbDotNodes.toString();
    }

    @Override
    protected String visualizeEdge(Object sId, Object eId, String flowRule) {
        return "    " + format(sId) + " -> " + format(eId) + " [label=\"" + flowRule + "\"];";
    }

    @Override
    public String visualizeBlock(Block bb, @Nullable Analysis<V, S, T> analysis) {
        return super.visualizeBlockHelper(bb, analysis, getSeparator());
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return super.visualizeSpecialBlockHelper(sbb);
    }

    @Override
    public String visualizeConditionalBlock(ConditionalBlock cbb) {
        // No extra content in DOT output.
        return "";
    }

    @Override
    public String visualizeBlockTransferInputBefore(Block bb, Analysis<V, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(
                VisualizeWhere.BEFORE, bb, analysis, getSeparator());
    }

    @Override
    public String visualizeBlockTransferInputAfter(Block bb, Analysis<V, S, T> analysis) {
        return super.visualizeBlockTransferInputHelper(
                VisualizeWhere.AFTER, bb, analysis, getSeparator());
    }

    /**
     * Create a dot file and return its name.
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
            String clsName = cfgStatement.getSimpleClassName();
            outFile.append(clsName);
            outFile.append("-initializer-");
            outFile.append(ast.getUid());

            srcLoc.append("<");
            srcLoc.append(clsName);
            srcLoc.append("::initializer::");
            srcLoc.append(((JCTree) cfgStatement.getCode()).pos);
            srcLoc.append(">");
        } else if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            CFGMethod cfgMethod = (CFGMethod) ast;
            String clsName = cfgMethod.getSimpleClassName();
            String methodName = cfgMethod.getMethodName();
            StringJoiner params = new StringJoiner(",");
            for (VariableTree tree : cfgMethod.getMethod().getParameters()) {
                params.add(tree.getType().toString());
            }
            outFile.append(clsName);
            outFile.append("-");
            outFile.append(methodName);
            if (params.length() != 0) {
                outFile.append("-");
                outFile.append(params);
            }

            srcLoc.append("<");
            srcLoc.append(clsName);
            srcLoc.append("::");
            srcLoc.append(methodName);
            srcLoc.append("(");
            srcLoc.append(params);
            srcLoc.append(")::");
            srcLoc.append(((JCTree) cfgMethod.getMethod()).pos);
            srcLoc.append(">");
        } else if (ast.getKind() == UnderlyingAST.Kind.LAMBDA) {
            CFGLambda cfgLambda = (CFGLambda) ast;
            String clsName = cfgLambda.getSimpleClassName();
            String methodName = cfgLambda.getMethodName();
            long uid = TreeUtils.treeUids.get(cfgLambda.getCode());
            outFile.append(clsName);
            outFile.append("-");
            outFile.append(methodName);
            outFile.append("-");
            outFile.append(uid);

            srcLoc.append("<");
            srcLoc.append(clsName);
            srcLoc.append("::");
            srcLoc.append(methodName);
            srcLoc.append("(");
            srcLoc.append(cfgLambda.getMethod().getParameters());
            srcLoc.append(")::");
            srcLoc.append(((JCTree) cfgLambda.getCode()).pos);
            srcLoc.append(">");
        } else {
            throw new BugInCF("Unexpected AST kind: " + ast.getKind() + " value: " + ast);
        }
        if (checkerName != null && !checkerName.isEmpty()) {
            outFile.append('-');
            outFile.append(checkerName);
        }
        outFile.append(".dot");

        // make path safe for Windows
        String outFileName = outFile.toString().replace("<", "_").replace(">", "");

        generated.put(srcLoc.toString(), outFileName);

        return outFileName;
    }

    @Override
    protected String format(Object obj) {
        return escapeString(obj);
    }

    @Override
    public String visualizeStoreThisVal(V value) {
        return storeEntryIndent + "this > " + escapeString(value);
    }

    @Override
    public String visualizeStoreLocalVar(LocalVariable localVar, V value) {
        return storeEntryIndent + localVar + " > " + escapeString(value);
    }

    @Override
    public String visualizeStoreFieldVal(FieldAccess fieldAccess, V value) {
        return storeEntryIndent + fieldAccess + " > " + escapeString(value);
    }

    @Override
    public String visualizeStoreArrayVal(ArrayAccess arrayValue, V value) {
        return storeEntryIndent + arrayValue + " > " + escapeString(value);
    }

    @Override
    public String visualizeStoreMethodVals(MethodCall methodCall, V value) {
        return storeEntryIndent + escapeString(methodCall) + " > " + escapeString(value);
    }

    @Override
    public String visualizeStoreClassVals(ClassName className, V value) {
        return storeEntryIndent + className + " > " + escapeString(value);
    }

    @Override
    public String visualizeStoreKeyVal(String keyName, Object value) {
        return storeEntryIndent + keyName + " = " + value;
    }

    /**
     * Escape the input String.
     *
     * @param str the string to be escaped
     * @return the escaped version of the string
     */
    private static String escapeString(final String str) {
        return str.replace("\"", "\\\"").replace("\r", "\\\\r").replace("\n", "\\\\n");
    }

    /**
     * Escape the double quotes from the string representation of the given object.
     *
     * @param obj an object
     * @return an escaped version of the string representation of the object
     */
    private static String escapeString(final Object obj) {
        return escapeString(String.valueOf(obj));
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
        return "}";
    }
}
