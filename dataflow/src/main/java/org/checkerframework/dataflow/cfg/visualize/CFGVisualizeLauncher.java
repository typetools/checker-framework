package org.checkerframework.dataflow.cfg.visualize;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.CFGProcessor;
import org.checkerframework.dataflow.cfg.CFGProcessor.CFGProcessResult;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;

/**
 * Launcher to generate the DOT or String representation of the control flow graph of a given method
 * in a given class.
 *
 * <p>Usage: Directly run it as the main class to generate the DOT representation of the control
 * flow graph of a given method in a given class. See {@link
 * org.checkerframework.dataflow.cfg.playground.ConstantPropagationPlayground} for another way to
 * use it.
 */
public class CFGVisualizeLauncher {

  /**
   * The main entry point of CFGVisualizeLauncher.
   *
   * @param args the passed arguments, see {@link #printUsage()} for the usage
   */
  public static void main(String[] args) {
    CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
    if (args.length == 0) {
      cfgVisualizeLauncher.printUsage();
      System.exit(1);
    }
    String input = args[0];
    File file = new File(input);
    if (!file.canRead()) {
      cfgVisualizeLauncher.printError("Cannot read input file: " + file.getAbsolutePath());
      cfgVisualizeLauncher.printUsage();
      System.exit(1);
    }

    String method = "test";
    String clas = "Test";
    String output = ".";
    boolean pdf = false;
    boolean error = false;
    boolean verbose = false;
    boolean string = false;

    for (int i = 1; i < args.length; i++) {
      switch (args[i]) {
        case "--outputdir":
          if (i >= args.length - 1) {
            cfgVisualizeLauncher.printError("Did not find <outputdir> after --outputdir.");
            continue;
          }
          i++;
          output = args[i];
          break;
        case "--pdf":
          pdf = true;
          break;
        case "--method":
          if (i >= args.length - 1) {
            cfgVisualizeLauncher.printError("Did not find <name> after --method.");
            continue;
          }
          i++;
          method = args[i];
          break;
        case "--class":
          if (i >= args.length - 1) {
            cfgVisualizeLauncher.printError("Did not find <name> after --class.");
            continue;
          }
          i++;
          clas = args[i];
          break;
        case "--verbose":
          verbose = true;
          break;
        case "--string":
          string = true;
          break;
        default:
          cfgVisualizeLauncher.printError("Unknown command line argument: " + args[i]);
          error = true;
          break;
      }
    }

    if (error) {
      System.exit(1);
    }

    if (!string) {
      cfgVisualizeLauncher.generateDOTofCFGWithoutAnalysis(
          input, output, method, clas, pdf, verbose);
    } else {
      String stringGraph =
          cfgVisualizeLauncher.generateStringOfCFGWithoutAnalysis(input, method, clas, verbose);
      System.out.println(stringGraph);
    }
  }

  /**
   * Generate the DOT representation of the CFG for a method, only. Does no dataflow analysis.
   *
   * @param inputFile java source input file
   * @param outputDir output directory
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param pdf also generate a PDF
   * @param verbose show verbose information in CFG
   */
  protected void generateDOTofCFGWithoutAnalysis(
      String inputFile,
      String outputDir,
      String method,
      String clas,
      boolean pdf,
      boolean verbose) {
    generateDOTofCFG(inputFile, outputDir, method, clas, pdf, verbose, null);
  }

  /**
   * Generate the String representation of the CFG for a method, only. Does no dataflow analysis.
   *
   * @param inputFile java source input file
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param verbose show verbose information in CFG
   * @return the String representation of the CFG
   */
  protected String generateStringOfCFGWithoutAnalysis(
      String inputFile, String method, String clas, boolean verbose) {
    @Nullable Map<String, Object> res = generateStringOfCFG(inputFile, method, clas, verbose, null);
    if (res != null) {
      String stringGraph = (String) res.get("stringGraph");
      if (stringGraph == null) {
        return "Unexpected output from generating string control flow graph, shouldn't be null.";
      }
      return stringGraph;
    } else {
      return "Unexpected output from generating string control flow graph, shouldn't be null.";
    }
  }

  /**
   * Generate the DOT representation of the CFG for a method.
   *
   * @param <V> the abstract value type to be tracked by the analysis
   * @param <S> the store type used in the analysis
   * @param <T> the transfer function type that is used to approximated runtime behavior
   * @param inputFile java source input file
   * @param outputDir source output directory
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param pdf also generate a PDF
   * @param verbose show verbose information in CFG
   * @param analysis analysis to perform before the visualization (or {@code null} if no analysis is
   *     to be performed)
   */
  public <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
      void generateDOTofCFG(
          String inputFile,
          String outputDir,
          String method,
          String clas,
          boolean pdf,
          boolean verbose,
          @Nullable Analysis<V, S, T> analysis) {
    ControlFlowGraph cfg = generateMethodCFG(inputFile, clas, method);
    if (analysis != null) {
      analysis.performAnalysis(cfg);
    }

    Map<String, Object> args = new HashMap<>(2);
    args.put("outdir", outputDir);
    args.put("verbose", verbose);

    CFGVisualizer<V, S, T> viz = new DOTCFGVisualizer<>();
    viz.init(args);
    Map<String, Object> res = viz.visualize(cfg, cfg.getEntryBlock(), analysis);
    viz.shutdown();

    if (pdf && res != null) {
      assert res.get("dotFileName") != null : "@AssumeAssertion(nullness): specification";
      producePDF((String) res.get("dotFileName"));
    }
  }

  /**
   * Generate the control flow graph of a method in a class.
   *
   * @param file java source input file
   * @param clas name of the class which includes the method to generate the CFG for
   * @param method name of the method to generate the CFG for
   * @return control flow graph of the specified method
   */
  protected ControlFlowGraph generateMethodCFG(String file, String clas, final String method) {

    CFGProcessor cfgProcessor = new CFGProcessor(clas, method);

    Context context = new Context();
    Options.instance(context).put("compilePolicy", "ATTR_ONLY");
    JavaCompiler javac = new JavaCompiler(context);

    JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class);

    JavaFileObject l = fileManager.getJavaFileObjectsFromStrings(List.of(file)).iterator().next();

    PrintStream err = System.err;
    try {
      // redirect syserr to nothing (and prevent the compiler from issuing
      // warnings about our exception.
      System.setErr(
          new PrintStream(
              new OutputStream() {
                @Override
                public void write(int b) throws IOException {}
              }));
      javac.compile(List.of(l), List.of(clas), List.of(cfgProcessor), List.nil());
    } catch (Throwable e) {
      // ok
    } finally {
      System.setErr(err);
    }

    CFGProcessResult res = cfgProcessor.getCFGProcessResult();

    if (res == null) {
      printError("internal error in type processor! method typeProcessOver() doesn't get called.");
      System.exit(1);
    }

    if (!res.isSuccess()) {
      printError(res.getErrMsg());
      System.exit(1);
    }

    return res.getCFG();
  }

  /**
   * Invoke "dot" command to generate a PDF.
   *
   * @param file name of the dot file
   */
  protected void producePDF(String file) {
    try {
      String command = "dot -Tpdf \"" + file + "\" -o \"" + file + ".pdf\"";
      Process child = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      child.waitFor();
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Generate the String representation of the CFG for a method.
   *
   * @param <V> the abstract value type to be tracked by the analysis
   * @param <S> the store type used in the analysis
   * @param <T> the transfer function type that is used to approximated runtime behavior
   * @param inputFile java source input file
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param verbose show verbose information in CFG
   * @param analysis analysis to perform before the visualization (or {@code null} if no analysis is
   *     to be performed)
   * @return a map which includes a key "stringGraph" and the String representation of CFG as the
   *     value
   */
  public <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
      @Nullable Map<String, Object> generateStringOfCFG(
      String inputFile,
      String method,
      String clas,
      boolean verbose,
      @Nullable Analysis<V, S, T> analysis) {
    ControlFlowGraph cfg = generateMethodCFG(inputFile, clas, method);
    if (analysis != null) {
      analysis.performAnalysis(cfg);
    }

    Map<String, Object> args = Collections.singletonMap("verbose", verbose);

    CFGVisualizer<V, S, T> viz = new StringCFGVisualizer<>();
    viz.init(args);
    Map<String, Object> res = viz.visualize(cfg, cfg.getEntryBlock(), analysis);
    viz.shutdown();
    return res;
  }

  /** Print usage information. */
  protected void printUsage() {
    System.out.println(
        "Generate the control flow graph of a Java method, represented as a DOT or String graph.");
    System.out.println(
        "Parameters: <inputfile> [--outputdir <outputdir>] [--method <name>] [--class <name>]"
            + " [--pdf] [--verbose] [--string]");
    System.out.println(
        "    --outputdir: The output directory for the generated files (defaults to '.').");
    System.out.println("    --method:    The method to generate the CFG for (defaults to 'test').");
    System.out.println(
        "    --class:     The class in which to find the method (defaults to 'Test').");
    System.out.println("    --pdf:       Also generate the PDF by invoking 'dot'.");
    System.out.println("    --verbose:   Show the verbose output (defaults to 'false').");
    System.out.println(
        "    --string:    Print the string representation of the control flow graph (defaults to"
            + " 'false').");
  }

  /**
   * Print error message.
   *
   * @param string error message
   */
  protected void printError(@Nullable String string) {
    System.err.println("ERROR: " + string);
  }
}
