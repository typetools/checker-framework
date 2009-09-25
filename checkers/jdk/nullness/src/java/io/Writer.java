package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class Writer implements Appendable, Closeable, Flushable {
  public void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public abstract void write(char[] a1, int a2, int a3) throws java.io.IOException;
  public void write(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(java.lang.String a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.Writer append(@Nullable java.lang.CharSequence a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.Writer append(@Nullable java.lang.CharSequence a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.Writer append(@Nullable char a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public abstract void flush() throws java.io.IOException;
  public abstract void close() throws java.io.IOException;
}
