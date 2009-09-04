package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Throwable{
  public Throwable() { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable java.lang.String a1, @Nullable java.lang.Throwable a2) { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable java.lang.Throwable a1) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getMessage() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getLocalizedMessage() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Throwable getCause() { throw new RuntimeException("skeleton method"); }
  public synchronized @PolyNull java.lang.Throwable initCause(@Nullable java.lang.Throwable a1) @PolyNull { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public void printStackTrace() { throw new RuntimeException("skeleton method"); }
  public void printStackTrace(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void printStackTrace(java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StackTraceElement[] getStackTrace() { throw new RuntimeException("skeleton method"); }
  public void setStackTrace(java.lang.StackTraceElement[] a1) { throw new RuntimeException("skeleton method"); }

    public synchronized native Throwable fillInStackTrace();
    private native int getStackTraceDepth();
    private native StackTraceElement getStackTraceElement(int index);

}
