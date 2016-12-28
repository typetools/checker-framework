package java.lang;

import org.checkerframework.checker.lock.qual.*;


import java.io.*;
import java.util.Properties;
// import java.util.PropertyPermission;
// import java.util.StringTokenizer;
// import java.security.AccessController;
// import java.security.PrivilegedAction;
// import java.security.AllPermission;
import java.nio.channels.Channel;
// import java.nio.channels.spi.SelectorProvider;
// import sun.nio.ch.Interruptible;
// import sun.reflect.Reflection;
// import sun.security.util.SecurityConstants;
// import sun.reflect.annotation.AnnotationType;

public final class System {
  protected System() {}
  public final static InputStream in = nullInputStream();
  public final static PrintStream out = nullPrintStream();
  public final static PrintStream err = nullPrintStream();
  public static void setIn(InputStream in) { throw new RuntimeException("skeleton method"); }
  public static void setOut(PrintStream out) { throw new RuntimeException("skeleton method"); }
  public static void setErr(PrintStream err) { throw new RuntimeException("skeleton method"); }
  public static Console console() { throw new RuntimeException("skeleton method"); }
  public static Channel inheritedChannel() throws IOException { throw new RuntimeException("skeleton method"); }
  public static void setSecurityManager(SecurityManager s) { throw new RuntimeException("skeleton method"); }
  public static SecurityManager getSecurityManager() { throw new RuntimeException("skeleton method"); }
  public static native long currentTimeMillis();
  public static native long nanoTime();
   public static native void arraycopy(@GuardSatisfied Object src, int srcPos, @GuardSatisfied Object dest, int destPos, int length);
   public static native int identityHashCode(@GuardSatisfied Object x);
  public static Properties getProperties() { throw new RuntimeException("skeleton method"); }
  public static void setProperties(Properties props) { throw new RuntimeException("skeleton method"); }
   public static String getProperty(String key) { throw new RuntimeException("skeleton method"); }
   public static String getProperty(String key, String def) { throw new RuntimeException("skeleton method"); }
  public static String setProperty(String key, String value) { throw new RuntimeException("skeleton method"); }
  public static String clearProperty(String key) { throw new RuntimeException("skeleton method"); }
  public static String getenv(String name) { throw new RuntimeException("skeleton method"); }
  public static java.util.Map<String,String> getenv() { throw new RuntimeException("skeleton method"); }
  public static void exit(int status) { throw new RuntimeException("skeleton method"); }
  public static void gc() { throw new RuntimeException("skeleton method"); }
  public static void runFinalization() { throw new RuntimeException("skeleton method"); }
  @Deprecated public static void runFinalizersOnExit(boolean value) { throw new RuntimeException("skeleton method"); }
  public static void load(String filename) { throw new RuntimeException("skeleton method"); }
  public static void loadLibrary(String libname) { throw new RuntimeException("skeleton method"); }
  public static native String mapLibraryName(String libname);
  @SuppressWarnings("rawtypes")
  static Class getCallerClass() { throw new RuntimeException("skeleton method"); }

   /**
     * The following two methods exist because in, out, and err must be
     * initialized to null.  The compiler, however, cannot be permitted to
     * inline access to them, since they are later set to more sensible values
     * by initializeSystemClass().
     */
    private static InputStream nullInputStream() throws NullPointerException {
        if (currentTimeMillis() > 0) {
            return null;
        }
        throw new NullPointerException();
    }

    private static PrintStream nullPrintStream() throws NullPointerException {
        if (currentTimeMillis() > 0) {
            return null;
        }
        throw new NullPointerException();
    }

}
