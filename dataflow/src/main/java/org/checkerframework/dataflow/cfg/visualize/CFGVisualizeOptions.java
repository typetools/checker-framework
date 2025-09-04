package org.checkerframework.dataflow.cfg.visualize;

import java.io.File;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Options for running analysis on files.
 *
 * <p>Usage: An instance of this class is created by calling {@link #parseArgs(String[])} with the
 * command line arguments. The arguments are parsed and the options are stored in the instance. They
 * can be retrieved by calling the appropriate getter method. See {@link
 * org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher} for an example.
 */
public class CFGVisualizeOptions {

  /** Default method name. */
  private static final String DEFAULT_METHOD = "test";

  /** Default class name. */
  private static final String DEFAULT_CLASS = "Test";

  /** Default output directory. */
  private static final String DEFAULT_OUTPUT_DIR = ".";

  /** The input file. */
  private final String input;

  /** The output directory. */
  private final String output;

  /** The method name. */
  private final String method;

  /** The class name. */
  private final String clas;

  /** True if the PDF should be generated. */
  private final boolean pdf;

  /** True if the verbose output should be generated. */
  private final boolean verbose;

  /** True if the string representation should be generated. */
  private final boolean string;

  /**
   * Private constructor.
   *
   * <p>This constructor is private to ensure that the object is only created by calling {@link
   * #parseArgs(String[])}.
   *
   * @param input the input file
   * @param output the output directory
   * @param method the method name
   * @param clas the class name
   * @param pdf true if the PDF should be generated
   * @param verbose true if the verbose output should be generated
   * @param string true if the string representation should be generated
   */
  private CFGVisualizeOptions(
      String input,
      String output,
      String method,
      String clas,
      boolean pdf,
      boolean verbose,
      boolean string) {
    this.input = input;
    this.output = output;
    this.method = method;
    this.clas = clas;
    this.pdf = pdf;
    this.verbose = verbose;
    this.string = string;
  }

  /**
   * Parse the command line arguments.
   *
   * <p>This method calls System.exit(1) if there are no arguments or if the input file cannot be
   * read.
   *
   * @param args command-line arguments, see {@link #printUsage()}
   * @return CFGVisualizeOptions object containing the parsed options
   */
  public static CFGVisualizeOptions parseArgs(String[] args) {
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    }
    String input = args[0];
    File file = new File(input);
    if (!file.canRead()) {
      printError("Cannot read input file: " + file.getAbsolutePath());
      printUsage();
      System.exit(1);
    }

    String method = DEFAULT_METHOD;
    String clas = DEFAULT_CLASS;
    String output = DEFAULT_OUTPUT_DIR;
    boolean pdf = false;
    boolean error = false;
    boolean verbose = false;
    boolean string = false;

    for (int i = 1; i < args.length; i++) {
      switch (args[i]) {
        case "--outputdir":
          if (i >= args.length - 1) {
            printError("Did not find <outputdir> after --outputdir.");
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
            printError("Did not find <name> after --method.");
            continue;
          }
          i++;
          method = args[i];
          break;
        case "--class":
          if (i >= args.length - 1) {
            printError("Did not find <name> after --class.");
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
          printError("Unknown command line argument: " + args[i]);
          error = true;
          break;
      }
    }

    if (error) {
      System.exit(1);
    }

    return new CFGVisualizeOptions(input, output, method, clas, pdf, verbose, string);
  }

  /**
   * Getter for the input file.
   *
   * @return the input file
   */
  public String getInputFile() {
    return input;
  }

  /**
   * Getter for the output directory.
   *
   * @return the output directory
   */
  public String getOutputDirectory() {
    return output;
  }

  /**
   * Getter for the method name.
   *
   * @return the method name
   */
  public String getMethodName() {
    return method;
  }

  /**
   * Getter for the class name.
   *
   * @return the class name
   */
  public String getClassName() {
    return clas;
  }

  /**
   * Returns true if PDF output should be generated.
   *
   * @return true if PDF output should be generated
   */
  public boolean isPdfOutput() {
    return pdf;
  }

  /**
   * Returns true if the string representation should be generated.
   *
   * @return true if the string representation should be generated
   */
  public boolean isStringOutput() {
    return string;
  }

  /**
   * Returns true if the verbose output should be generated.
   *
   * @return true if the verbose output should be generated
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Print usage information.
   *
   * <p>Prints usage information to System.out.
   */
  private static void printUsage() {
    System.out.println(
        "Generate the control flow graph of a Java method, represented as a DOT or String"
            + " graph.");
    System.out.println(
        "Parameters: <inputfile> [--outputdir <outputdir>] [--method <name>] [--class"
            + " <name>] [--pdf] [--verbose] [--string]");
    System.out.println(
        "  --outputdir: The output directory for the generated files (defaults to '.').");
    System.out.println("  --method:  The method to generate the CFG for (defaults to 'test').");
    System.out.println("  --class:   The class in which to find the method (defaults to 'Test').");
    System.out.println("  --pdf:     Also generate the PDF by invoking 'dot'.");
    System.out.println("  --verbose:   Show the verbose output (defaults to 'false').");
    System.out.println(
        "  --string:  Print the string representation of the control flow graph"
            + " (defaults to 'false').");
  }

  /**
   * Print error message.
   *
   * <p>Sends the error message to System.err.
   *
   * @param string error message
   */
  private static void printError(@Nullable String string) {
    System.err.println("ERROR: " + string);
  }
}
