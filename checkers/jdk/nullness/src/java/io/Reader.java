package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class Reader implements Readable, Closeable {
  protected Reader() {}
  public int read(java.nio.CharBuffer a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(char[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public abstract int read(char[] a1, int a2, int a3) throws IOException;
  public long skip(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean ready() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public void mark(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public abstract void close() throws IOException;
}
