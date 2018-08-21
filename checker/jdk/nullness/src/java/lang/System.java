package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.TerminatesExecution;

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
  public static @Nullable Console console() { throw new RuntimeException("skeleton method"); }
  public static @Nullable Channel inheritedChannel() throws IOException { throw new RuntimeException("skeleton method"); }
  public static void setSecurityManager(@Nullable SecurityManager s) { throw new RuntimeException("skeleton method"); }
  public static @Nullable SecurityManager getSecurityManager() { throw new RuntimeException("skeleton method"); }
  public static native long currentTimeMillis();
  public static native long nanoTime();
  @SideEffectFree public static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
  @Pure public static native int identityHashCode(@Nullable Object x);
  public static Properties getProperties() { throw new RuntimeException("skeleton method"); }
  public static void setProperties(@Nullable Properties props) { throw new RuntimeException("skeleton method"); }
  @Pure public static @Nullable String getProperty(String key) { throw new RuntimeException("skeleton method"); }
  @Pure public static @PolyNull String getProperty(String key, @PolyNull String def) { throw new RuntimeException("skeleton method"); }
  public static @Nullable String setProperty(String key, String value) { throw new RuntimeException("skeleton method"); }
  public static @Nullable String clearProperty(String key) { throw new RuntimeException("skeleton method"); }
  public static @Nullable String getenv(String name) { throw new RuntimeException("skeleton method"); }
  public static java.util.Map<String,String> getenv() { throw new RuntimeException("skeleton method"); }
  @TerminatesExecution
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
