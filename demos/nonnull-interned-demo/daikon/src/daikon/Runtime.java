package daikon;

import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.io.*;


/**
 * The Runtime class provides methods for printing values to a Daikon data
 * trace file.  Daikon can process the data trace information, either while
 * the target program is running or after-the-fact, to produce likely
 * invariants.
 * <p>
 *
 * The Daikon front end for Java, named Chicory, modifies the target
 * program by inserting calls to the printing routines of the Runtime
 * class.  Neither Chicory nor Daikon calls the methods of the Runtime
 * class; only the target program (as instrumented by Chicory) does, in
 * order to create input to Daikon.
 * <p>
 **/
public final class Runtime {

  private static final String lineSep = System.getProperty("line.separator");

  // Constructor
  private Runtime() {
    throw new Error("Do not create instances of Runtime");
  }

  ////////////////////////////////////////////////////////////////////////
  /// Fresh (unique) classname used to disambiguate overloaded method
  /// calls during instrumentation; is not instantiated or used.
  public static class Unique {}
  public static final Unique unique = null;

  ////////////////////////////////////////////////////////////////////////
  /// The context-sensitive instrumentation pass creates bodies for
  /// abstract methods that throw this very object; that way we don't
  /// have to inspect their return type since they never return.
  /// Thanks to this global instance, they don't need to call "new" either.
  public static class AbstractException extends Error {
    static final long serialVersionUID = 20020130L;
  }
  public static final AbstractException abstractException =
    new AbstractException();


  ///////////////////////////////////////////////////////////////////////////
  /// Timestamps
  ///

  // This is used as this_invocation_nonce (and is incremented after use).
  // Uses of it should be synchronized (probably on dtrace).
  public static int time = 0;


  ///////////////////////////////////////////////////////////////////////////
  /// Classname utilities
  ///

  // This section is lifted from utilMDE/UtilMDE.java and should be kept
  // in synch with that version.

  private static HashMap<String,String> primitiveClassesFromJvm = new HashMap<String,String>(8);
  static {
    primitiveClassesFromJvm.put("Z", "boolean");
    primitiveClassesFromJvm.put("B", "byte");
    primitiveClassesFromJvm.put("C", "char");
    primitiveClassesFromJvm.put("D", "double");
    primitiveClassesFromJvm.put("F", "float");
    primitiveClassesFromJvm.put("I", "int");
    primitiveClassesFromJvm.put("J", "long");
    primitiveClassesFromJvm.put("S", "short");
  }

  /**
   * Convert a classname from JVML format to Java format.
   * For example, convert "[Ljava/lang/Object;" to "java.lang.Object[]".
   **/
  public static String classnameFromJvm(String classname) {
    int dims = 0;
    while (classname.startsWith("[")) {
      dims++;
      classname = classname.substring(1);
    }
    String result;
    if (classname.startsWith("L") && classname.endsWith(";")) {
      result = classname.substring(1, classname.length() - 1);
      result = result.replace('/', '.');
    } else {
      result = primitiveClassesFromJvm.get(classname);
      if (result == null) {
        // As a failsafe, use the input; perhaps it is in Java, not JVML,
        // format.
        result = classname;
        // throw new Error("Malformed base class: " + classname);
      }
    }
    for (int i=0; i<dims; i++) {
      result += "[]";
    }
    return result;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Printing
  ///

  // Note the global variables:  dangerous.

  // This flag is used to suppress output during reentrant
  // instrumentation; for example, when outputting the elements of an
  // instrumented List-derived object, we don't want to start
  // outputting the program points in the size()/get() methods of the
  // List object.
  // The instrumentation code sets and checks this; it's more efficient
  // to do that once, rather than on every call to print.
  // This variable should not be modified unless the lock on
  // daikon.Runtime.dtrace is held.
  public static int ps_count = 0;

  public static int dtraceLimit = Integer.MAX_VALUE; // a number of records
  public static int printedRecords = 0;
  public static boolean dtraceLimitTerminate = false;


  // Inline this?  Probably not worth it.
  // Increment the number of records that have been printed.
  public static void incrementRecords() {
    printedRecords++;
    if (printedRecords >= dtraceLimit) {
      noMoreOutput();
    }
  }

  // Ensures that no more dtrace output will occur.  May terminate Java.
  public static void noMoreOutput() {
    // The incrementRecords method (which calls this) is called inside a
    // synchronized block, but re-synchronize just to be sure, or in case
    // this is called from elsewhere.
    synchronized ( daikon.Runtime.dtrace ) {
      // The shutdown hook is synchronized on this, so close it up
      // ourselves, lest the call to System.exit cause deadlock.
      dtrace.println();
      dtrace.println("# EOF (added by no_more_output)");
      dtrace.close();

      // Don't set dtrace to null, because if we continue running, there will
      // be many attempts to synchronize on it.  (Is that a performance
      // bottleneck, if we continue running?)
      // dtrace = null;
      dtrace_closed = true;

      if (dtraceLimitTerminate) {
        // System.err.println("Printed " + printedRecords + " records.  Exiting.");
        // System.exit(1);
        throw new Daikon.TerminationMessage("Printed " + printedRecords + " records.  Exiting.");
      } else {
        // By default, no special output if the system continues to run.
        // System.err.println("Printed " + printedRecords + " records.  No more Daikon output.");
        // prevent any future output
        no_dtrace = true;
        ps_count++;
      }
    }
  }

  // It's convenient to have an entire run in one data trace file, so
  // probably don't bother to generalize this to put output from a single
  // run in different files depending on the class the information is
  // about.
  public static PrintStream dtrace;
  public static boolean dtrace_closed = false;
  // daikon.Daikon should never load daikon.Runtime; but sometimes it
  // happens, due to reflective loading of the target program that gets the
  // instrumented target program.  The instrumented program has a static
  // block that invokes daikon.Runtime.
  public static boolean no_dtrace = false;
    // This initializer doesn't work because findLoadedClass is a protected
    // method, so instead make clients set no_dtrace explicitly.
    // = (ClassLoader.getSystemClassLoader().findLoadedClass("daikon.Daikon")
    //    != null);

  public static void setDtrace(String filename, boolean append) {
    // System.out.printf("entered daikon.Runtime.setDtrace(%s, %b)%n", filename, append);
    if (no_dtrace) {
      throw new Error("setDtrace called when no_dtrace was specified");
    }
    try {
      File file = new File(filename);
      File parent = file.getParentFile();
      if (parent != null) parent.mkdirs();
      OutputStream os = new FileOutputStream(filename, append);
      if (filename.endsWith(".gz")) {
        if (append)
          throw new Error("DTRACEAPPEND environment variable is set." + lineSep
                          + "Cannot append to gzipped dtrace file " + filename);
        os = new GZIPOutputStream(os);
      }
      dtraceLimit = Integer.getInteger("DTRACELIMIT", Integer.MAX_VALUE).intValue();
      dtraceLimitTerminate = Boolean.getBoolean("DTRACELIMITTERMINATE");
      // 8192 is the buffer size in BufferedReader
      BufferedOutputStream bos = new BufferedOutputStream(os, 8192);
      dtrace = new PrintStream(bos);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e);
    }
    if (supportsAddShutdownHook()) {
      addShutdownHook();
    } else {
      System.err.println("Warning: .dtrace file may be incomplete if program is aborted");
    }
    // System.out.printf("exited daikon.Runtime.setDtrace(%s, %b)%n", filename, append);
  }

  /**
   * If the current data trace file is not yet set, then set it.
   * The value of the DTRACEFILE environment variable is used;
   * if that environment variable is not set, then the argument
   * to this method is used instead.
   **/
  public static void setDtraceMaybe(String default_filename) {
    // System.out.println("setDtraceMaybe(" + default_filename + "); old = " + dtrace);
    if ((dtrace == null) && (! no_dtrace)) {
      // Jeremy used "daikon.dtrace.filename".
      String filename = System.getProperty("DTRACEFILE", default_filename);
      boolean append = System.getProperty("DTRACEAPPEND") != null;
      setDtrace(filename, append);
    }
  }

  private static boolean supportsAddShutdownHook() {
    try {
      Class<java.lang.Runtime> rt = java.lang.Runtime.class;
      rt.getMethod("addShutdownHook", new Class[] {
        java.lang.Thread.class
      });
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // Add a shutdown hook to close the PrintStream when the program
  // exits
  private static void addShutdownHook() {
    java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          if (! dtrace_closed) {

            // When the program being instrumented exits, the buffers
            // of the "dtrace" (PrintStream) object are not flushed,
            // so we miss the tail of the file.

            synchronized (daikon.Runtime.dtrace) {
              dtrace.println();
              // This lets us know we didn't lose any data.
              dtrace.println("# EOF (added by daikon.Runtime.addShutdownHook)");
              dtrace.close();
            }
          }
        }
      });
  }

  // This is no longer necessary, as it was for Daikon-jtb
  // // This is a dummy method that can be called from Java code instead of
  // //   SomeClass.daikonPrint
  // // because daikonPrint doesn't (yet) exist in SomeClass.java.
  // // Later we will fix up all references to this.
  // public static void daikonPrint_dummy(Object x, PrintStream ps, int depth, String prefix, String target) {
  //   throw new Error("Unreplaced call to DaikonRuntime.daikonPrint_dummy(" + x + ", " + ps + ", " + depth + ", " + prefix + ", " + target + ")");
  // }


  // Some of these functions could be open-coded, but I don't want to get
  // into the business of writing lots of bytecodes; let the JIT inline
  // them.

  // The other advantage to dynamic generation is that it works for
  // arbitrary types, not just those hard-coded here.  That is a big
  // advantage.


  ///////////////////////////////////////////////////////////////////////////
  /// print
  ///

  // I used to have overloaded print and println methods (that called
  // print_Object and print_String respectively), but that could give me
  // unexpected results if the Object I was trying to print happened to be
  // a String.  So now I use different names to avoid that problem.

  public static final void print_Object(java.io.PrintStream ps, Object x) {
    if (x == null) {
      ps.print("null");
    } else {
      ps.print(java.lang.System.identityHashCode(x));
    }
  }

  // augmentation of print_Object above
  public static final void print_class(java.io.PrintStream ps, Object x) {
    if (x == null) {
      ps.print("null");
    } else {
      print_String(ps, classnameFromJvm(x.getClass().getName()));
    }
  }

  public static final void println_modbit_modified(java.io.PrintStream ps) {
    ps.println("1");          // "modified"
  }

  public static final void println_modbit_missing(java.io.PrintStream ps) {
    ps.println("2");          // "missing"
  }

  public static final void println_class_and_modbit(java.io.PrintStream ps, Object x) {
    if (x == null) {
      ps.println("nonsensical");
      println_modbit_missing(ps);
    } else {
      println_String(ps, classnameFromJvm(x.getClass().getName()));
      println_modbit_modified(ps);
    }
  }

  // Avoid using this; prefer print_quoted_String instead, unless we can
  // guarantee that the string contains no character that need to be quoted.
  public static final void print_String(java.io.PrintStream ps, String x) {
    ps.print((x == null) ? "null" : "\"" + x + "\"");
  }

  public static final void print_quoted_String(java.io.PrintStream ps, String x) {
    ps.print((x == null) ? "null" : "\"" + quote(x) + "\"");
  }

  public static final void println_quoted_String_and_modbit(java.io.PrintStream ps, String x) {
    if (x == null) {
      ps.println("nonsensical");
      println_modbit_missing(ps);
    } else {
      println_quoted_String(ps, x);
      println_modbit_modified(ps);
    }
  }

  // Not yet used; but probably should be.
  public static final void print_quoted_Character(java.io.PrintStream ps, Character ch) {
    ps.print((ch == null) ? "null" : quote(ch));
  }

  // Lifted directly from utilMDE/UtilMDE.java, where it is called
  // escapeNonJava(), but repeated here to make this class self-contained.
  /** Quote \, ", \n, and \r characters in the target; return a new string. **/
  public static String quote(String orig) {
    StringBuffer sb = new StringBuffer();
    // The previous escape (or escaped) character was seen right before
    // this position.  Alternately:  from this character forward, the string
    // should be copied out verbatim (until the next escaped character).
    int post_esc = 0;
    int orig_len = orig.length();
    for (int i=0; i<orig_len; i++) {
      char c = orig.charAt(i);
      switch (c) {
      case '\"':
      case '\\':
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append('\\');
        post_esc = i;
        break;
      case '\n':                // not lineSep
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append("\\n");       // not lineSep
        post_esc = i+1;
        break;
      case '\r':
        if (post_esc < i) {
          sb.append(orig.substring(post_esc, i));
        }
        sb.append("\\r");
        post_esc = i+1;
        break;
      default:
        // Do nothing; i gets incremented.
      }
    }
    if (sb.length() == 0)
      return orig;
    sb.append(orig.substring(post_esc));
    return sb.toString();
  }


  // The overhead of this is too high to call in quote(String)
  public static String quote(Character ch) {
    char c = ch.charValue();
    switch (c) {
    case '\"':
      return "\\\"";
    case '\\':
      return "\\\\";
    case '\n':                  // not lineSep
      return "\\n";             // not lineSep
    case '\r':
      return "\\r";
    default:
      return new String(new char[] { c });
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// println
  ///

  public static final void println_Object(java.io.PrintStream ps, Object x) {
    print_Object(ps, x);
    ps.println();
  }

  public static final void println_class(java.io.PrintStream ps, Object x) {
    print_class(ps, x);
    ps.println();
  }

  // Avoid using this; prefer println_quoted_String instead.
  public static final void println_String(java.io.PrintStream ps, String x) {
    print_String(ps, x);
    ps.println();
  }

  public static final void println_quoted_String(java.io.PrintStream ps, String x) {
    print_quoted_String(ps, x);
    ps.println();
  }

  ///////////////////////////////////////////////////////////////////////////
  /// println_array
  ///

  // These are all cut-and-paste (the code is identical in some cases).

  ///
  /// Object
  ///

  public static final void println_array_Object(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      print_Object(ps, a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        print_Object(ps, a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_Object(java.io.PrintStream ps, List<?> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      print_Object(ps, v.get(0));
      for (int i=1; i<size; i++) {
        ps.print(' ');
        print_Object(ps, v.get(i));
      }
    }
    ps.println(']');
  }

  // Deprecated
  // Print an array of the classes of the elements.
  public static final void println_array_Object_eltclass(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      print_class(ps, a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        print_class(ps, a[i]);
      }
    }
    ps.println(']');
  }

  // Print an array of the classes of the elements.
  public static final void println_array_Object_eltclass_and_modbit(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("nonsensical");
      println_modbit_missing(ps);
      return;
    }
    boolean any_null = false;
    ps.print('[');
    if (a.length > 0) {
      print_class(ps, a[0]);
      any_null = (a[0] == null);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        print_class(ps, a[i]);
        any_null |= (a[i] == null);
      }
    }
    ps.println(']');
    println_modbit_modified(ps);
  }

  // Deprecated.
  // Print an array of the classes of the elements.
  public static final void println_array_Object_eltclass(java.io.PrintStream ps, List<?> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      print_class(ps, v.get(0));
      for (int i=1; i<size; i++) {
        ps.print(' ');
        print_class(ps, v.get(i));
      }
    }
    ps.println(']');
  }

  // Print an array of the classes of the elements.
  public static final void println_array_Object_eltclass_and_modbit(java.io.PrintStream ps, List<?> v) {
    if (v == null) {
      ps.println("nonsensical");
      println_modbit_missing(ps);
      return;
    }
    boolean any_null = false;
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      print_class(ps, v.get(0));
      any_null = (v.get(0) == null);
      for (int i=1; i<size; i++) {
        ps.print(' ');
        print_class(ps, v.get(i));
        any_null |= (v.get(i) == null);
      }
    }
    ps.println(']');
    println_modbit_modified(ps);
  }

  // The parsing routines can't deal with "missing" in the middle of an
  // array (I think), so if an element is null, use 0 for its length.
  // (A better solution be to mark the "length" derived variable as missing.
  // For expediency, I'm not doing that right now.  -MDE 2/1/2004)

  // Print the lengths of the elements of the top-level array.
  // This is for Object[][] or for anything[][][], where "anything" may
  // be either Object or a base class.
  public static final void println_array_2d_size(java.io.PrintStream ps, Object[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0] == null ? 0 : a[0].length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i] == null ? 0 : a[i].length);
      }
    }
    ps.println(']');
  }


  ///
  /// List
  ///

  // Print the lengths of the elements of a List[]

  public static final void println_array_List_size(java.io.PrintStream ps, List[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0] == null ? 0 : a[0].size());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i] == null ? 0 : a[i].size());
      }
    }
    ps.println(']');
  }

  public static final void println_array_List_size(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0] == null ? 0 : ((List)a[0]).size());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i] == null ? 0 : ((List)a[i]).size());
      }
    }
    ps.println(']');
  }

  public static final void println_array_List_size(java.io.PrintStream ps, List<List<?>> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0) == null ? 0 : v.get(0).size());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i) == null ? 0 : v.get(i).size());
      }
    }
    ps.println(']');
  }

  ///
  /// String
  ///

  public static final void println_array_String(java.io.PrintStream ps, String[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      print_quoted_String(ps, a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        print_quoted_String(ps, a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_String(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      print_quoted_String(ps, (String)a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        print_quoted_String(ps, (String)a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_String(java.io.PrintStream ps, List<String> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      print_quoted_String(ps, v.get(0));
      for (int i=1; i<size; i++) {
        ps.print(' ');
        print_quoted_String(ps, v.get(i));
      }
    }
    ps.println(']');
  }

  ///
  /// Primitive types (mostly numbers)
  ///

  // The primitive types are:
  //   boolean byte char double float int long short
  // Each of the sections should be identical up to renaming the primitive
  // types, so if one is changed, all the others should be, too.

  /// boolean

  public static final void println_array_boolean(java.io.PrintStream ps, boolean[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_boolean(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Boolean)a[0]).booleanValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Boolean)a[i]).booleanValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_boolean(java.io.PrintStream ps, List<Boolean> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).booleanValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).booleanValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, boolean[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// byte

  public static final void println_array_byte(java.io.PrintStream ps, byte[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_byte(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Byte)a[0]).byteValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Byte)a[i]).byteValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_byte(java.io.PrintStream ps, List<Byte> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).byteValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).byteValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, byte[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// char

  public static final void println_array_char(java.io.PrintStream ps, char[] a) {
    println_array_char_as_String(ps, a);
  }

  public static final void println_array_char(java.io.PrintStream ps, Object[] a) {
    println_array_char_as_chars(ps, a);
  }

  public static final void println_array_char(java.io.PrintStream ps, List<Character> v) {
    println_array_char_as_chars(ps, v);
  }

  public static final void println_array_char_as_String(java.io.PrintStream ps, char[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    println_quoted_String(ps, new String(a));
  }

  // Outputs a sequence of space-separated characters, with (only) return
  // and newline quoted.  (Should backslash also be quoted?)
  public static final void println_array_char_as_chars(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    for (int i=0; i<a.length; i++) {
      if (i != 0)
        ps.print(' ');
      char c = ((Character)a[0]).charValue();
      if (c == '\r')
        ps.print("\\r");
      else if (c == '\n')       // not lineSep
        ps.print("\\n");        // not lineSep
      else
        ps.print(c);
    }
    ps.println(']');
  }

  // Outputs a sequence of space-separated characters, with (only) return
  // and newline quoted.  (Should backslash also be quoted?)
  public static final void println_array_char_as_chars(java.io.PrintStream ps, List<Character> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    for (int i=0; i<size; i++) {
      if (i != 0)
        ps.print(' ');
      char c = v.get(i).charValue();
      if (c == '\r')
        ps.print("\\r");
      else if (c == '\n')       // not lineSep
        ps.print("\\n");        // not lineSep
      else
        ps.print(c);
    }
    ps.println(']');
  }

  public static final void println_array_char_as_ints(java.io.PrintStream ps, char[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(Character.getNumericValue(a[0]));
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(Character.getNumericValue(a[i]));
      }
    }
    ps.println(']');
  }

  public static final void println_array_char_as_ints(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(Character.getNumericValue(((Character)a[0]).charValue()));
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(Character.getNumericValue(((Character)a[i]).charValue()));
      }
    }
    ps.println(']');
  }

  public static final void println_array_char_as_ints(java.io.PrintStream ps, List<Character> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(Character.getNumericValue(v.get(0).charValue()));
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(Character.getNumericValue(v.get(i).charValue()));
      }
    }
    ps.println(']');
  }

  // I'm not sure if this is what I want -- I might prefer to view it as String[].
  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, char[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// double

  public static final void println_array_double(java.io.PrintStream ps, double[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_double(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Double)a[0]).doubleValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Double)a[i]).doubleValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_double(java.io.PrintStream ps, List<Double> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).doubleValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).doubleValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, double[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// float

  public static final void println_array_float(java.io.PrintStream ps, float[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_float(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Float)a[0]).floatValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Float)a[i]).floatValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_float(java.io.PrintStream ps, List<Float> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).floatValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).floatValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, float[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// int

  public static final void println_array_int(java.io.PrintStream ps, int[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_int(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Integer)a[0]).intValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Integer)a[i]).intValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_int(java.io.PrintStream ps, List<Integer> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).intValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).intValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, int[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// long

  public static final void println_array_long(java.io.PrintStream ps, long[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_long(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Long)a[0]).longValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Long)a[i]).longValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_long(java.io.PrintStream ps, List<Long> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).longValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).longValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, long[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }

  /// short

  public static final void println_array_short(java.io.PrintStream ps, short[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(a[0]);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(a[i]);
      }
    }
    ps.println(']');
  }

  public static final void println_array_short(java.io.PrintStream ps, Object[] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print(((Short)a[0]).shortValue());
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print(((Short)a[i]).shortValue());
      }
    }
    ps.println(']');
  }

  public static final void println_array_short(java.io.PrintStream ps, List<Short> v) {
    if (v == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    int size = v.size();
    if (size > 0) {
      ps.print(v.get(0).shortValue());
      for (int i=1; i<size; i++) {
        ps.print(' ');
        ps.print(v.get(i).shortValue());
      }
    }
    ps.println(']');
  }

  // Print the lengths of the elements of the top-level array.
  public static final void println_array_2d_size(java.io.PrintStream ps, short[][] a) {
    if (a == null) {
      ps.println("null");
      return;
    }
    ps.print('[');
    if (a.length > 0) {
      ps.print((a[0]).length);
      for (int i=1; i<a.length; i++) {
        ps.print(' ');
        ps.print((a[i]).length);
      }
    }
    ps.println(']');
  }


  ///////////////////////////////////////////////////////////////////////////
  /// BytesHelper
  ///

  // Not currently used (2/19/2005).

  // From: package org.hibernate.util;

  public static final int toInt( byte[] bytes ) {
    int result = 0;
    for (int i=0; i<4; i++) {
      result = ( result << 8 ) - Byte.MIN_VALUE + (int) bytes[i];
    }
    return result;
  }

  public static short toShort( byte[] bytes ) {
    return (short) ( ( ( - (short) Byte.MIN_VALUE + (short) bytes[0] ) << 8  )
                     - (short) Byte.MIN_VALUE + (short) bytes[1] );
  }

  public static final byte[] toBytes(int value) {
    byte[] result = new byte[4];
    for (int i=3; i>=0; i--) {
      result[i] = (byte) ( ( 0xFFl & value ) + Byte.MIN_VALUE );
      value >>>= 8;
    }
    return result;
  }

  public static byte[] toBytes(short value) {
    byte[] result = new byte[2];
    for (int i=1; i>=0; i--) {
      result[i] = (byte) ( ( 0xFFl & value )  + Byte.MIN_VALUE );
      value >>>= 8;
    }
    return result;
  }

  // More efficient version that doesn't allocate a lot of arrays.
  static final byte[] toBytesStaticResult = new byte[4];
  private static final void toBytesStatic(int value) {
    for (int i=3; i>=0; i--) {
      toBytesStaticResult[i] = (byte) ( ( 0xFFl & value ) + Byte.MIN_VALUE );
      value >>>= 8;
    }
  }

  private static final void printIntBytes(PrintStream ps, int value) {
    toBytesStatic(value);
    try {
      ps.write(toBytesStaticResult);
    } catch (IOException e) {
      throw new Error(e);
    }
  }

}
