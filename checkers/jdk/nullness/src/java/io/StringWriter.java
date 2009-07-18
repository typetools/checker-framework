package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class StringWriter extends Writer {
  public StringWriter() { throw new RuntimeException("skeleton method"); }
  public StringWriter(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void write(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void write(java.lang.String a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public java.io.StringWriter append(@Nullable java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public java.io.StringWriter append(@Nullable java.lang.CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public java.io.StringWriter append(char a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer getBuffer() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
