package org.checkerframework.dataflow.cfg.visualize;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.CFGProcessor;
import org.checkerframework.dataflow.cfg.CFGProcessor.CFGProcessResult;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.plumelib.util.ArrayMap;

/**
 * Launcher to generate the DOT or String representation of the control flow graph of a given method
 * in a given class.
 *
 * <p>Usage: Directly run it as the main class to generate the DOT representation of the control
 * flow graph of a given method in a given class. See {@link
 * org.checkerframework.dataflow.cfg.playground.ConstantPropagationPlayground} for another way to
 * use it.
 */
public final class CFGVisualizeLauncher {

  /** Class cannot be instantiated. */
  private CFGVisualizeLauncher() {
    throw new AssertionError("Class CFGVisualizeLauncher cannot be instantiated.");
  }

  /**
   * The main entry point of CFGVisualizeLauncher.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

    performAnalysis(config, null);
  }

  /**
   * Generate a visualization of the CFG of a method, with an optional analysis.
   *
   * @param <V> the abstract value type of the analysis
   * @param <S> the store type of the analysis
   * @param <T> the transfer function type of the analysis
   * @param config CFGVisualizeOptions that includes input file, output directory, method name, and
   *     class name
   * @param analysis analysis to perform before the visualization (or {@code null} if no analysis is
   *     to be performed)
   */
  public static <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
      void performAnalysis(CFGVisualizeOptions config, @Nullable Analysis<V, S, T> analysis) {
    if (!config.isString()) {
      if (analysis == null) {
        generateDOTofCFGWithoutAnalysis(
            config.getInputFile(),
            config.getOutputDirectory(),
            config.getMethodName(),
            config.getClassName(),
            config.isPDF(),
            config.isVerbose());
      } else {
        generateDOTofCFG(
            config.getInputFile(),
            config.getOutputDirectory(),
            config.getMethodName(),
            config.getClassName(),
            config.isPDF(),
            config.isVerbose(),
            analysis);
      }
    } else {
      if (analysis == null) {
        String stringGraph =
            generateStringOfCFGWithoutAnalysis(
                config.getInputFile(),
                config.getMethodName(),
                config.getClassName(),
                config.isVerbose());
        System.out.println(stringGraph);
      } else {
        Map<String, Object> res =
            generateStringOfCFG(
                config.getInputFile(),
                config.getMethodName(),
                config.getClassName(),
                config.isVerbose(),
                analysis);
        if (res != null) {
          String stringGraph = (String) res.get("stringGraph");
          if (stringGraph == null) {
            System.err.println(
                "Unexpected output from generating string control flow graph, shouldn't be"
                    + " null. Result map: "
                    + res);
            return;
          }
          System.out.println(stringGraph);
        } else {
          System.err.println(
              "Unexpected output from generating string control flow graph, shouldn't be"
                  + " null.");
        }
      }
    }
  }

  /**
   * Generate the DOT representation of the CFG for a method, only. Does no dataflow analysis.
   *
   * @param inputFile a Java source file, used as input
   * @param outputDir output directory
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param pdf also generate a PDF
   * @param verbose show verbose information in CFG
   */
  private static void generateDOTofCFGWithoutAnalysis(
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
   * @param inputFile a Java source file, used as input
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param verbose show verbose information in CFG
   * @return the String representation of the CFG
   */
  private static String generateStringOfCFGWithoutAnalysis(
      String inputFile, String method, String clas, boolean verbose) {
    Map<String, Object> res = generateStringOfCFG(inputFile, method, clas, verbose, null);
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
   * @param <T> the transfer function type that is used to approximate run-time behavior
   * @param inputFile a Java source file, used as input
   * @param outputDir source output directory
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param pdf also generate a PDF
   * @param verbose show verbose information in CFG
   * @param analysis analysis to perform before the visualization (or {@code null} if no analysis is
   *     to be performed)
   */
  private static <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
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

    Map<String, Object> args = new ArrayMap<>(2);
    args.put("outdir", outputDir);
    args.put("verbose", verbose);

    CFGVisualizer<V, S, T> viz = new DOTCFGVisualizer<>();
    viz.init(args);
    Map<String, Object> res = viz.visualizeWithAction(cfg, cfg.getEntryBlock(), analysis);
    viz.shutdown();

    if (pdf && res != null) {
      assert res.get("dotFileName") != null : "@AssumeAssertion(nullness): specification";
      producePDF((String) res.get("dotFileName"));
    }
  }

  /**
   * Generate the control flow graph of a method in a class.
   *
   * @param file a Java source file, used as input
   * @param clas name of the class which includes the method to generate the CFG for
   * @param method name of the method to generate the CFG for
   * @return control flow graph of the specified method
   */
  public static ControlFlowGraph generateMethodCFG(String file, String clas, String method) {
    CFGProcessor cfgProcessor = new CFGProcessor(clas, method);

    Context context = new Context();
    Options.instance(context).put("compilePolicy", "ATTR_ONLY");
    JavaCompiler javac = new JavaCompiler(context);

    JavaFileObject l;
    try (@SuppressWarnings("mustcall:type.arguments.not.inferred" // Context isn't annotated for
        // the Must Call Checker.
        )
        JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class)) {
      l = fileManager.getJavaFileObjectsFromStrings(List.of(file)).iterator().next();
    } catch (IOException e) {
      throw new Error(e);
    }

    PrintStream err = System.err;
    try {
      // Redirect syserr to nothing (and prevent the compiler from issuing
      // warnings about our exception).
      @SuppressWarnings({
        "builder:required.method.not.called",
        "mustcall:assignment"
      }) // Won't be needed in JDK 11+ with use of "OutputStream.nullOutputStream()".
      @MustCall() OutputStream nullOS =
          // In JDK 11+, this can be just "OutputStream.nullOutputStream()".
          new OutputStream() {
            @Override
            public void write(int b) throws IOException {}
          };
      System.setErr(new PrintStream(nullOS));
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
   * Write generated String representation of the CFG for a method to a file.
   *
   * @param inputFile a Java source file, used as input
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param outputFile source output file
   * @param analysis instance of forward or backward analysis from specific dataflow test case
   */
  @SuppressWarnings("CatchAndPrintStackTrace") // we want to use e.printStackTrace here.
  public static void writeStringOfCFG(
      String inputFile, String method, String clas, String outputFile, Analysis<?, ?, ?> analysis) {
    Map<String, Object> res = generateStringOfCFG(inputFile, method, clas, true, analysis);
    try (FileWriter out = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
      if (res != null && res.get("stringGraph") != null) {
        out.write(res.get("stringGraph").toString());
      }
      out.write(System.lineSeparator());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Invoke "dot" command to generate a PDF.
   *
   * @param file name of the dot file
   */
  private static void producePDF(String file) {
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
   * @param <T> the transfer function type that is used to approximate run-time behavior
   * @param inputFile a Java source file, used as input
   * @param method name of the method to generate the CFG for
   * @param clas name of the class which includes the method to generate the CFG for
   * @param verbose show verbose information in CFG
   * @param analysis analysis to perform before the visualization (or {@code null} if no analysis is
   *     to be performed)
   * @return a map which includes a key "stringGraph" and the String representation of CFG as the
   *     value
   */
  private static <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
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

  /**
   * Print error message.
   *
   * @param string error message
   */
  private static void printError(@Nullable String string) {
    System.err.println("ERROR: " + string);
  }
}
