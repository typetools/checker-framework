package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Throwable implements java.io.Serializable{
    private static final long serialVersionUID = 0L;
  public Throwable() { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable String a1, @Nullable Throwable a2) { throw new RuntimeException("skeleton method"); }
  public Throwable(@Nullable Throwable a1) { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable String getMessage() { throw new RuntimeException("skeleton method"); }
  public @Pure @Nullable String getLocalizedMessage() { throw new RuntimeException("skeleton method"); }
  public @Nullable Throwable getCause() { throw new RuntimeException("skeleton method"); }
  public synchronized @PolyRaw Throwable initCause(@PolyRaw Throwable this, @Nullable Throwable a1) { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public void printStackTrace() { throw new RuntimeException("skeleton method"); }
  public void printStackTrace(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public void printStackTrace(java.io.PrintWriter a1) { throw new RuntimeException("skeleton method"); }
  public StackTraceElement[] getStackTrace() { throw new RuntimeException("skeleton method"); }
  public void setStackTrace(StackTraceElement[] a1) { throw new RuntimeException("skeleton method"); }

    public synchronized native Throwable fillInStackTrace();
    private native int getStackTraceDepth();
    private native StackTraceElement getStackTraceElement(int index);

}
