package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Runtime{
  protected Runtime() {}
  public static Runtime getRuntime() { throw new RuntimeException("skeleton method"); }
  public void exit(int a1) { throw new RuntimeException("skeleton method"); }
  public void addShutdownHook(Thread a1) { throw new RuntimeException("skeleton method"); }
  public boolean removeShutdownHook(Thread a1) { throw new RuntimeException("skeleton method"); }
  public void halt(int a1) { throw new RuntimeException("skeleton method"); }
  public static void runFinalizersOnExit(boolean a1) { throw new RuntimeException("skeleton method"); }
  public Process exec(String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public Process exec(String a1, String @Nullable [] a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public Process exec(String a1, String @Nullable [] a2, @Nullable java.io.File a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public Process exec(String[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public Process exec(String[] a1, String @Nullable [] a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public Process exec(String[] a1, String @Nullable [] a2, @Nullable java.io.File a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void runFinalization() { throw new RuntimeException("skeleton method"); }
  public void load(String a1) { throw new RuntimeException("skeleton method"); }
  public void loadLibrary(String a1) { throw new RuntimeException("skeleton method"); }
  public java.io.InputStream getLocalizedInputStream(java.io.InputStream a1) { throw new RuntimeException("skeleton method"); }
  public java.io.OutputStream getLocalizedOutputStream(java.io.OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public native int availableProcessors();
  public native long freeMemory();
  public native long totalMemory();
  public native long maxMemory();
  public native void gc();
  private static native void runFinalization0();
  public native void traceInstructions(boolean on);
  public native void traceMethodCalls(boolean on);

}
