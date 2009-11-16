package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StackTraceElement implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public StackTraceElement(java.lang.String a1, java.lang.String a2, @Nullable java.lang.String a3, int a4) { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getFileName() { throw new RuntimeException("skeleton method"); }
  public int getLineNumber() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getClassName() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getMethodName() { throw new RuntimeException("skeleton method"); }
  public boolean isNativeMethod() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
}
