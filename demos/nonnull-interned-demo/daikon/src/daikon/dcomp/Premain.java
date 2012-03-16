package daikon.dcomp;

import java.lang.instrument.*;
import java.security.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import utilMDE.*;
import daikon.chicory.DaikonVariableInfo;

import daikon.DynComp;

public class Premain {

  /**
   * Set of pre_instrumented jdk classes.  Needed so that we will instrument
   * classes generated on the fly in the jdk.
   **/
  static Set<String> pre_instrumented = new LinkedHashSet<String>();

  public static void premain (String agentArgs, Instrumentation inst)
    throws IOException {

    Options options = new Options (DynComp.usage_synopsis, DynComp.class, Premain.class);
    String[] args = options.parse_and_usage (agentArgs.split ("  *"));
    if (args.length > 0) {
      options.print_usage ("Unexpected argument %s", args[0]);
      System.exit (-1);
    }

    DaikonVariableInfo.std_visibility = DynComp.std_visibility;
    DCRuntime.depth = DynComp.nesting_depth;

    if (DynComp.no_jdk)
      DCInstrument.jdk_instrumented = false;

    if (DynComp.verbose) {
      System.out.format ("In dcomp premain, agentargs ='%s', " +
                       "Instrumentation = '%s'\n", agentArgs, inst);
      System.out.printf ("Options settings: %n%s%n", options.settings());
    }

    // Read in the list of pre-instrumented classes
    if (!DynComp.no_jdk) {
      InputStream strm
        = Object.class.getResourceAsStream ("jdk_classes.txt");
      if (strm == null) {
        System.err.println("Can't find jdk_classes.txt; see Daikon manual, section \"Instrumenting the JDK with DynComp\"");
        System.exit(1);
      }
      BufferedReader reader
        = new BufferedReader (new InputStreamReader (strm));
      while (true) {
        String line = reader.readLine();
        if (line == null)
          break;
        // System.out.printf ("adding '%s'%n", line);
        pre_instrumented.add (line);
      }
    }

    // Find out what classes are already loaded
    Class[] loaded_classes = inst.getAllLoadedClasses();
    for (Class loaded_class : loaded_classes) {
      // System.out.printf ("loaded class = %s\n", loaded_class.getName());
    }

    // Setup the shutdown hook
    Thread shutdown_thread = new ShutdownThread();
    java.lang.Runtime.getRuntime().addShutdownHook (shutdown_thread);

    Transform transformer = new Transform();
    inst.addTransformer (transformer);

    // Initialize the static tag array
    DCRuntime.init();


  }

  static public class Transform implements ClassFileTransformer {

    File debug_dir;
    File debug_bin_dir;
    File debug_orig_dir;

    public Transform() {
      debug_dir = DynComp.debug_dir;
      debug_bin_dir = new File (debug_dir, "bin");
      debug_orig_dir = new File (debug_dir, "orig");

      if (DynComp.debug) {
        debug_bin_dir.mkdirs();
        debug_orig_dir.mkdirs();
      }
    }

    public byte[] transform (ClassLoader loader, String className,
                           Class<?> classBeingRedefined,
                           ProtectionDomain protectionDomain,
                           byte[] classfileBuffer)
                                  throws IllegalClassFormatException {

      // Don't instrument JDK classes (but allow instrumentation of the java
      // compiler)
      if ((className.startsWith ("java/") || className.startsWith ("com/")
           || className.startsWith ("sun/"))
          && !className.startsWith ("com/sun/tools/javac")) {
        if (DynComp.no_jdk || pre_instrumented.contains (className))
          return (null);
        if (DynComp.verbose)
          System.out.printf ("Instrumenting JDK class %s%n", className);
      }

      // Don't instrument our own classes
      if ((className.startsWith ("daikon/dcomp/")
           && !className.startsWith ("daikon/dcomp/Test"))
          || className.startsWith ("utilMDE")
          || className.startsWith ("daikon/chicory/"))
        return (null);

      if (DynComp.verbose)
        System.out.format ("In Transform: class = %s\n", className);

      try {
        // Parse the bytes of the classfile, die on any errors
        ClassParser parser = new ClassParser
          (new ByteArrayInputStream (classfileBuffer), className);
        JavaClass c = parser.parse();


        if (DynComp.debug) {
          c.dump (new File (debug_orig_dir, c.getClassName() + ".class"));
        }

        // Transform the file
        DCInstrument dci = new DCInstrument (c, false, loader);
        JavaClass njc = dci.instrument();
        if (njc == null) {
          if (DynComp.verbose)
            System.out.printf ("Didn't instrument %s%n", c.getClassName());
          return (null);
        } else {
          if (DynComp.debug) {
            System.out.printf ("Dumping to %s%n", debug_bin_dir);
            njc.dump (new File (debug_bin_dir, njc.getClassName() + ".class"));
            BCELUtil.dump (njc, debug_bin_dir);
          }
          return (njc.getBytes());
        }
      } catch (Throwable e) {
        System.out.printf ("Unexpected Error: %n");
        e.printStackTrace();
        throw new RuntimeException ("Unexpected error", e);
      }
    }
  }

  /**
   * Shutdown thread that writes out the comparability results
   */
  public static class ShutdownThread extends Thread {

    public void run() {

      // If requested, write the comparability data to a file
      if (!DynComp.no_cset_file) {
        if (DynComp.compare_sets_file != null) {
          if (DynComp.verbose)
            System.out.println ("Writing comparability sets to "
                                + DynComp.compare_sets_file);
          PrintWriter compare_out = open (DynComp.compare_sets_file);
          Stopwatch watch = new Stopwatch();
          DCRuntime.print_all_comparable (compare_out);
          compare_out.close();
          if (DynComp.verbose)
            System.out.printf ("Comparability sets written in %s%n",
                               watch.format());
        } else {
          System.out.println ("Writing comparability sets to standard output");
          DCRuntime.print_all_comparable (new PrintWriter(System.out, true));
        }
      }
      
      if (DynComp.trace_sets_file != null) {
        if (DynComp.verbose)
          System.out.println ("Writing traced comparability sets to "
                              + DynComp.trace_sets_file);
        PrintWriter trace_out = open (DynComp.trace_sets_file);
        Stopwatch watch = new Stopwatch();
        DCRuntime.trace_all_comparable (trace_out);
        trace_out.close();
        if (DynComp.verbose)
          System.out.printf ("Comparability sets written in %s%n",
                             watch.format());
      } else {
        // Writing comparability sets to standard output?
      }

      if (DynComp.verbose)
        DCRuntime.decl_stats();

      // Write the decl file out
      File decl_file = DynComp.decl_file;
      if (decl_file == null) {
        decl_file = new File (DynComp.output_dir, "comparability.decls");
      }
      if (DynComp.verbose)
        System.out.println("Writing decl file to " + decl_file);
      PrintWriter decl_fp = open (decl_file);
      Stopwatch watch = new Stopwatch();
      DCRuntime.print_decl_file (decl_fp);
      decl_fp.close();
      if (DynComp.verbose) {
        System.out.printf ("Decl file written in %s%n", watch.format());
        System.out.printf ("comp_list = %,d%n", DCRuntime.comp_list_ms);
        System.out.printf ("ppt name  = %,d%n", DCRuntime.ppt_name_ms);
        System.out.printf ("decl vars = %,d%n", DCRuntime.decl_vars_ms);
        System.out.printf ("total     = %,d%n", DCRuntime.total_ms);
      }
      if (DynComp.verbose)
        System.out.println ("DynComp complete");
    }
  }

  public static PrintWriter open (File filename) {
    try {
      return new PrintWriter (new BufferedWriter (new FileWriter (filename)));
      //return new PrintWriter (filename);
      //return new PrintStream (new BufferedWriter
      //            (new OutputStreamWriter (new FileOutputStream(filename))));
    } catch (Exception e) {
      throw new Error ("Can't open " + filename, e);
    }
  }
}
