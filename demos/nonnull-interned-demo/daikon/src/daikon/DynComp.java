package daikon;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import daikon.chicory.StreamRedirectThread;
import daikon.dcomp.*;
import utilMDE.*;

/**
 * This is the main class for DynComp. It uses the javaagent switch to
 * java (which allows classes to be instrumented as they are loaded).
 * This class parses the command line arguments and starts java with the
 * javaagent switch on the target program.
 * Code based largely on daikon.Chicory
 */
public class DynComp {

  @Option("-v Print information about the classes being transformed")
  public static boolean verbose = false;

  @Option("-d Dump the instrumented classes to disk")
  public static boolean debug = false;

  @Option("Directory in which to create debug files")
  public static File debug_dir = new File("debug");

  @Option("Directory in which to create output files")
  public static File output_dir = new File(".");

  @Option("-f Output filename for Daikon decl file")
  public static File decl_file = null;

  @Option("Don't output a comparability sets file")
  public static boolean no_cset_file = false;

  @Option("Output file for comparability sets")
  public static File compare_sets_file = null;

  @Option("Only process program points matching the regex")
  public static List<Pattern> ppt_select_pattern = new ArrayList<Pattern>();

  @Option("Ignore program points matching the regex")
  public static List<Pattern> ppt_omit_pattern = new ArrayList<Pattern>();

  @Option("Don't use an instrumented JDK")
  public static boolean no_jdk = false;

  @Option("jar file containing an instrumented JDK")
  public static File rt_file = null;

  @Option("use standard visibility")
  public static boolean std_visibility = false;

  @Option("variable nesting depth")
  public static int nesting_depth = 2;

  @Option("Shiny element output")
  public static boolean shiny_print = false;

  @Option("Trace output file")
  public static File trace_sets_file = null;

  @Option("Depth of call hierarchy for line tracing")
  public static int trace_line_depth = 1;

//  @Option("Enable tracing");
//  public static boolean tracing_enabled = true;

  public static String usage_synopsis
    = "java daikon.DynComp [options]";

  /**
   * Path to java agent jar file that performs the transformation.
   * The "main" procedure is Premain.premain().
   * @see Premain#premain
   **/
  @Option ("Path to the DynComp agent jar file")
  public static File premain = null;


  /** Thread that copies output from target to our output **/
  public static StreamRedirectThread out_thread;

  /** Thread that copies stderr from target to our stderr **/
  public static StreamRedirectThread err_thread;

  /** starting time (msecs) **/
  public static long start = System.currentTimeMillis();

  private static final SimpleLog basic = new SimpleLog (false);

  /** Synopsis for the dcomp command line **/
  public static final String synopsis
    = "daikon.DynComp [options] target [target-args]";

  /**
   * Entry point of DynComp <p>
   * @param args see usage for argument descriptions
   */
  public static void main(String[] args) {

    // Parse our arguments
    Options options = new Options (synopsis, DynComp.class);
    options.ignore_options_after_arg (true);
    String[] target_args = options.parse_and_usage (args);
    boolean ok = check_args (options, target_args);
    if (!ok)
      System.exit (1);

    // Turn on basic logging if the debug was selected
    basic.enabled = debug;
    basic.log ("target_args = %s%n", Arrays.toString (target_args));

    // Start the target.  Pass the same options to the premain as
    // were passed here.

    DynComp dcomp = new DynComp();
    dcomp.start_target (options.get_options_str(), target_args);
  }

  /**
   * Check the resulting arguments for legality.  Prints a message and
   * Returns false if there was an error
   */
  public static boolean check_args (Options options, String[] target_args) {

    // Make sure arguments have legal values
    if (nesting_depth < 0) {
      options.print_usage ("nesting depth (%d) must not be negative",
                           nesting_depth);
      return (false);
    }
    if (target_args.length == 0) {
      options.print_usage ("target program must be specified");
      return (false);
    }
    if (!no_jdk && rt_file != null && !rt_file.exists()) {
      // if --rt-file was given, but doesn't exist
      options.print_usage ("specified rt-file does not exist");
      return (false);
    }

    return (true);

  }

  /**
   * Starts the target program with the java agent setup to do the
   * transforms.  All java agent arguments are passed to it.  Our
   * classpath is passed to the new jvm
   */
  void start_target (String premain_args, String[] target_args) {

    String target_class = target_args[0].replaceFirst (".*[/.]", "");

    // Default the decls file to <target-program-name>.decls-DynComp
    if (decl_file == null) {
      decl_file = new File (String.format ("%s.decls-DynComp", target_class));
      premain_args = "--decl-file=" + decl_file + " " + premain_args;
    }

    // Get the current classpath
    String cp = System.getProperty("java.class.path");
    basic.log("classpath = '%s'\n", cp);
    if (cp == null)
      cp = ".";

    // The the separator for items in the class path
    String separator = System.getProperty("path.separator");
    basic.log("separator = %s\n", separator);
    if (separator == null)
      separator = ";"; //should work for windows at least...

    // Look for dcomp_premain.jar along the classpath
    if (premain == null)
      {
        String[] cpath = cp.split(separator);
        for (String path : cpath)
          {
            File poss_premain = new File(path, "dcomp_premain.jar");
            if (poss_premain.canRead())
              premain = poss_premain;
          }
      }

    // If not on the classpath look in $(DAIKONDIR)/java
    if (premain == null) {
      String daikon_dir = System.getenv ("DAIKONDIR");
      if (daikon_dir != null) {
        String file_separator = System.getProperty ("file.separator");
        File poss_premain = new File (daikon_dir + file_separator + "java",
                                      "dcomp_premain.jar");
        if (poss_premain.canRead())
          premain = poss_premain;
      }
    }

    // If we didn't find a premain, give up
    if (premain == null) {
      System.err.printf ("Can't find dcomp_premain.jar on the classpath\n");
      System.err.printf ("or in $DAIKONDIR/java\n");
      System.err.printf ("It should be found in directory where Daikon was "
                         + "installed\n");
      System.err.printf ("Use the --premain switch to specify its location\n");
      System.err.printf ("or change your classpath to include it\n");
      System.exit (1);
    }


    // Look for rt-file
    if (!no_jdk) {
      // Look for dcomp_rt.jar along the classpath
      if (rt_file == null)
      {
        String[] cpath = cp.split(separator);
        for (String path : cpath)
        {
          File poss_rt = new File(path, "dcomp_rt.jar");
          if (poss_rt.canRead())
            rt_file = poss_rt;
        }
      }

      // If not on the classpath look in $(DAIKONDIR)/java
      if (rt_file == null) {
        String daikon_dir = System.getenv ("DAIKONDIR");
        if (daikon_dir != null) {
          String file_separator = System.getProperty ("file.separator");
          File poss_rt = new File (daikon_dir + file_separator + "java",
                                   "dcomp_rt.jar");
          if (poss_rt.canRead())
            rt_file = poss_rt;
        }
      }

      // If we didn't find a rt-file, give up
      if (rt_file == null) {
        System.err.printf ("Can't find dcomp_rt.jar on the classpath "
                           + "or in $DAIKONDIR/java\n");
        System.err.printf ("Use the --rt-file switch to specify its location, "
                           + "or change your classpath to include it\n");
        System.err.printf ("See Daikon manual, section \"Instrumenting the "
                           + "JDK with DynComp\" for help\n");
        System.exit (1);
      }
    }


    // Build the command line to execute the target with the javaagent
    List<String> cmdlist = new ArrayList<String>();
    cmdlist.add ("java");

    cmdlist.add ("-cp");
    cmdlist.add (cp);
    cmdlist.add ("-ea");
    //    cmdlist.add ("-Xmx" + heap_size);
    if (!no_jdk)
      cmdlist.add ("-Xbootclasspath:" + rt_file + ":" + cp);

    cmdlist.add (String.format("-javaagent:%s=%s", premain, premain_args));

    for (String target_arg : target_args)
      cmdlist.add (target_arg);
    if (verbose)
      System.out.printf ("\nExecuting target program: %s\n",
                         args_to_string(cmdlist));
    String[] cmdline = new String[cmdlist.size()];
    cmdline = cmdlist.toArray(cmdline);

    // Execute the command, sending all output to our streams
    java.lang.Runtime rt = java.lang.Runtime.getRuntime();
    Process dcomp_proc = null;
    try {
      dcomp_proc = rt.exec(cmdline);
    }
    catch (Exception e) {
      System.out.printf("Exception '%s' while executing '%s'\n", e,
                        cmdline);
      System.exit(1);
    }
    int result = redirect_wait (dcomp_proc);

    // XXX check result!
  }


  /** Wait for stream redirect threads to complete **/
  public int redirect_wait (Process p) {

    // Create the redirect theads and start them
    StreamRedirectThread err_thread
      = new StreamRedirectThread("stderr", p.getErrorStream(), System.err);
    StreamRedirectThread out_thread
      = new StreamRedirectThread("stdout", p.getInputStream(), System.out);
    err_thread.start();
    out_thread.start();

    // Wait for the process to terminate and return the results
    int result = -1;
    while (true) {
      try {
        result = p.waitFor();
        break;
      } catch (InterruptedException e) {
        System.out.printf ("unexpected interrupt %s while waiting for "
                           + "target to finish", e);
      }
    }

    // Make sure all output is forwarded before we finish
    try {
      err_thread.join();
      out_thread.join();
    } catch (InterruptedException e) {
      System.out.printf ("unexpected interrupt %s while waiting for "
                         + "threads to join", e);
    }

    return (result);
  }

  /** Returns elapsed time as a String since the start of the program **/
  public static String elapsed()
  {
    return ("[" + (System.currentTimeMillis() - start) + " msec]");
  }

  public static long elapsed_msecs()
  {
    return (System.currentTimeMillis() - start);
  }

  /** convert a list of arguments into a command line string **/
  public String args_to_string(List<String> args)
  {
    String str = "";
    for (String arg : args)
      str += arg + " ";
    return (str.trim());
  }

}
