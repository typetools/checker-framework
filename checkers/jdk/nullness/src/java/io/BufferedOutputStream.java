package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class BufferedOutputStream extends FilterOutputStream {
  public BufferedOutputStream(java.io.OutputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public BufferedOutputStream(java.io.OutputStream a1, int a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public synchronized void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
