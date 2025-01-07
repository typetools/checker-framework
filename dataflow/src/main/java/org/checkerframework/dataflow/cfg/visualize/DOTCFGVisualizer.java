package org.checkerframework.dataflow.cfg.visualize;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
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
  public Map<String, Object> visualize(
      ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {
    String dotGraph = visualizeGraph(cfg, entry, analysis);

    Map<String, Object> vis = new HashMap<>(4);
    vis.put("dotGraph", dotGraph);
    return vis;
  }

  @Override
  public Map<String, Object> visualizeWithAction(
      ControlFlowGraph cfg, Block entry, @Nullable Analysis<V, S, T> analysis) {
    Map<String, Object> vis = visualize(cfg, entry, analysis);
    String dotGraph = (String) vis.get("dotGraph");
    if (dotGraph == null) {
      throw new BugInCF("dotGraph key missing in visualize result!");
    }
    String dotFileName = dotOutputFileName(cfg.underlyingAST);

    try (BufferedWriter out =
        new BufferedWriter(new FileWriter(dotFileName, StandardCharsets.UTF_8))) {
      out.write(dotGraph);
    } catch (IOException e) {
      throw new UserError("Error creating dot file (is the path valid?): " + dotFileName, e);
    }
    vis.put("dotFileName", dotFileName);
    return vis;
  }

  @SuppressWarnings("keyfor:enhancedfor")
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
        sbDotNodes.append(getProcessOrderSimpleString(processOrder.get(v))).append(getSeparator());
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
    return "    "
        + escapeString(sId)
        + " -> "
        + escapeString(eId)
        + " [label=\""
        + flowRule
        + "\"];";
  }

  @Override
  public String visualizeBlock(Block bb, @Nullable Analysis<V, S, T> analysis) {
    return super.visualizeBlockWithSeparator(bb, analysis, getSeparator());
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
    StringBuilder outFile = new StringBuilder();

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
      String enclosingMethodName = cfgLambda.getEnclosingMethodName();
      long uid = TreeUtils.treeUids.get(cfgLambda.getCode());
      outFile.append(clsName);
      outFile.append("-");
      if (enclosingMethodName != null) {
        outFile.append(enclosingMethodName);
        outFile.append("-");
      }
      outFile.append(uid);

      srcLoc.append("<");
      srcLoc.append(clsName);
      if (enclosingMethodName != null) {
        srcLoc.append("::");
        srcLoc.append(enclosingMethodName);
        srcLoc.append("(");
        @SuppressWarnings("nullness") // enclosingMethodName != null => getEnclosingMethod() != null
        @NonNull MethodTree method = cfgLambda.getEnclosingMethod();
        srcLoc.append(method.getParameters());
        srcLoc.append(")");
      }
      srcLoc.append("::");
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

    // make path safe for Linux
    if (outFile.length() > 255) {
      outFile.setLength(255);
    }
    // make path safe for Windows
    String outFileBaseName = outFile.toString().replace("<", "_").replace(">", "");
    String outFileName = outDir + "/" + outFileBaseName;

    generated.put(srcLoc.toString(), outFileName);

    return outFileName;
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
  @Override
  protected String escapeString(String str) {
    return str.replace("\"", "\\\"").replace("\r", "\\\\r").replace("\n", "\\\\n");
  }

  /**
   * Write a file {@code methods.txt} that contains a mapping from source code location to generated
   * dot file.
   */
  @Override
  public void shutdown() {
    // Open for append, in case of multiple sub-checkers.
    try (Writer fstream =
            Files.newBufferedWriter(
                Paths.get(outDir + "/methods.txt"),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        BufferedWriter out = new BufferedWriter(fstream)) {
      for (Map.Entry<String, String> kv : generated.entrySet()) {
        out.write(kv.getKey());
        out.append("\t");
        out.write(kv.getValue());
        out.append(lineSeparator);
      }
    } catch (IOException e) {
      throw new UserError(
          "Error creating methods.txt file in: " + outDir + "; ensure the path is valid", e);
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
