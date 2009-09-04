package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class OutputStream implements Closeable, Flushable {
  public OutputStream() { throw new RuntimeException("skeleton method"); }
  public abstract void write(int a1) throws java.io.IOException;
  public void write(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
