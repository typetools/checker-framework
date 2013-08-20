package java.lang;

import dataflow.quals.Pure;
import checkers.nullness.quals.Nullable;


public final class StackTraceElement implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public StackTraceElement(String a1, String a2, @Nullable String a3, int a4) { throw new RuntimeException("skeleton method"); }
  public @Nullable String getFileName() { throw new RuntimeException("skeleton method"); }
  public int getLineNumber() { throw new RuntimeException("skeleton method"); }
  public String getClassName() { throw new RuntimeException("skeleton method"); }
  public String getMethodName() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isNativeMethod() { throw new RuntimeException("skeleton method"); }
  @Pure public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
}
