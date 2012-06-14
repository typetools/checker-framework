package daikon.tools.jtb;

import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import daikon.*;
import jtb.syntaxtree.*;
import jtb.JavaParser;
import jtb.ParseException;
import utilMDE.*;
import java.util.*;

/**
 * Create a splitter info file from Java source.
 * <p>
 *
 * The argument is a list of .java files.  The original .java files are
 * left unmodified.  A .spinfo file is written for every .java file.
 */

public class CreateSpinfo {

// The expressions in the Java source are extracted as follows:
// For each method:
//  * extracts all expressions in conditional statements
//    ie. if, for, which, etc.
//  * if the method body is a one-line return statement, it
//    extracts it for later substitution into expressions which
//    call this function. These statements are referred to as
//    replace statements
// For each field declaration
//  * if the field is a boolean, it stores the expression
//    "<fieldname> == true" as a splitting condition.
//
//  The method printSpinfoFile prints out these expressions and
//  replace statements in splitter info file format.

  public static final Logger debug =
    Logger.getLogger("daikon.tools.jtb.CreateSpinfo");

  private static String usage =
    UtilMDE.joinLines(
      "Usage:  java daikon.tools.CreateSpinfo FILE.java ...",
      "  -o outputfile   Put all output in specified file",
      "  -h              Display this usage message"
      );


  public static void main (String[] args) throws IOException {
    try {
      mainHelper(args);
    } catch (Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args) throws IOException {

    // If not set, put output in files named after the input (source) files.
    String outputfilename = null;

    daikon.LogHelper.setupLogs (daikon.LogHelper.INFO);
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.help_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };

    Getopt g =
      new Getopt("daikon.tools.jtb.CreateSpinfo", args, "ho:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;
        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel (g.getOptarg(), LogHelper.FINE);
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     option_name);
        }
        break;
      case 'o':
        outputfilename = g.getOptarg();
        break;
      case 'h':
        System.out.println(usage);
        throw new Daikon.TerminationMessage();
      case '?':
        break; // getopt() already printed an error
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    // The index of the first non-option argument -- the name of the file
    int argindex = g.getOptind();
    if (argindex >= args.length) {
      throw new Daikon.TerminationMessage(
         "Error: No .java file arguments supplied.",
         usage);
    }
    if (outputfilename != null) {
      PrintWriter output = new PrintWriter(new FileWriter(outputfilename));
      for ( ; argindex < args.length; argindex++) {
        String javaFileName = args[argindex];
        writeSplitters(javaFileName, output);
      }
      output.flush();
      output.close();
    } else {
      for ( ; argindex < args.length; argindex++) {
        String javaFileName = args[argindex];
        String spinfoFileName = spinfoFileName(javaFileName);
        PrintWriter output = new PrintWriter(new FileWriter(spinfoFileName));
        writeSplitters(javaFileName, output);
        output.flush();
        output.close();
      }
    }
  }

  /**
   * Returns the default name for a spinfo file create from
   * a java file named javaFileName.
   * @param javaFileName the name of the java file from which
   *  this spinfo file is being created.
   */
  private static String spinfoFileName(String javaFileName) {
    if (javaFileName.endsWith(".java")) {
      return javaFileName.substring(0, javaFileName.length()-5) + ".spinfo";
    }

    // The file does not end with ".java".  Proceed, but issue a warning.
    System.err.println ("Warning: CreateSpinfo input file "
                        + javaFileName + "does not end in .java.");

    // change the file extension to .spinfo
    int dotPos = javaFileName.indexOf (".");
    if (dotPos == -1) {
      return javaFileName + ".spinfo";
    } else {
      return javaFileName.substring (0, dotPos) + ".spinfo";
    }
  }


  /**
   * Write splitters for the Java file to the PrintWriter as a spinfo file.
   * @param javaFileName the name of the java file from which this
   *  spinfo file is being made.
   * @param output the PrintWriter to which this spinfo file is being wrote.
   */
  private static void writeSplitters(String javaFileName, PrintWriter output)
    throws IOException {
    Reader input = new FileReader(javaFileName);
    JavaParser parser = new JavaParser(input);
    Node root = null;
    try {
      root = parser.CompilationUnit();
    } catch (ParseException e) {
      e.printStackTrace();
      throw new Daikon.TerminationMessage("ParseException");
    }
    debug.fine ("CreateSpinfo: processing file " + javaFileName);
    ConditionExtractor extractor = new ConditionExtractor();
    root.accept(extractor);
    // conditions: method name (String) to conditional expressions (String)
    Map<String,List<String>> conditions = extractor.getConditionMap();
    // replaceStatements: method declaration (String) to method body (String)
    Map<String,String> replaceStatements = extractor.getReplaceStatements();
    String packageName = extractor.getPackageName();
    addOrigConditions(conditions);
    printSpinfoFile(output, conditions, replaceStatements, packageName);
  }

  /**
   * For each condition in conditionMap, an additional condition is
   * added which is identical to the initial condition with the exception
   * that it is prefixed with "orig(" and suffixed with ")".
   */
  private static void addOrigConditions(Map<String,List<String>> conditionMap) {
    for (List<String> conditions : conditionMap.values() ) {
      int size = conditions.size();
      for (int i = 0; i < size; i++) {
        conditions.add(addOrig(conditions.get(i)));
      }
    }
  }

  /**
   * Returns condition prefixed with "orig(" and suffixed with ")".
   */
  private static String addOrig(String condition) {
    return "orig(" + condition + ")";
  }

  /**
   * Writes the spinfo file specified by conditions, replaceStatements, and
   * package name to output.
   * @param output the PrintWriter to which the spinfo file is to be written.
   * @param conditions the conditions to be included in the spinfo file.
   *  conditions should be a map from method names to the conditional
   *  expressions for that method to split upon.
   * @param replaceStatements the replace statements to be included in the
   *  spinfo file.  replaceStatements should be a map from method
   *  declarations to method bodies.
   * @param packageName the package name of the java file for which this
   *  spinfo file is being written.
   */
  private static void printSpinfoFile(PrintWriter output,
                                      Map<String,List<String>> conditions,
                                      Map<String,String> replaceStatements,
                                      String packageName)
    throws IOException {
    if (!replaceStatements.values().isEmpty()) {
      output.println("REPLACE");
      List<String> methodsList = new ArrayList<String>(replaceStatements.keySet());
      Collections.sort(methodsList);
      for (String declaration : methodsList) {
	output.println(declaration);
	output.println(removeNewlines(replaceStatements.get(declaration)));
      }
      output.println();
    }
    List<String> method_conds;
    List<String> methodsList = new ArrayList<String>(conditions.keySet());
    Collections.sort(methodsList);
    for (String method : methodsList) {
      method_conds = conditions.get(method);
      Collections.sort(method_conds);
      if (method_conds.size() > 0) {
	if (packageName != null) {
	  method = packageName + "." + method;
        }
	output.println("PPT_NAME " + method);
	for (int i = 0; i < method_conds.size(); i++) {
	  output.println(removeNewlines(method_conds.get(i)));
	}
	output.println();
      }
    }
  }

  /**
   * Returns target with line separators and the whitespace
   * around a line separator replaced by a single space.
   */
  private static String removeNewlines(String target) {
    String[] lines = UtilMDE.splitLines(target);
    for (int i=0; i<lines.length; i++) {
      lines[i] = lines[i].trim();
    }
    return UtilMDE.join(lines, " ");
  }

}
