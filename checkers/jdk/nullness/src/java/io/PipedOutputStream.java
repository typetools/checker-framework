package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedOutputStream extends OutputStream {
  public PipedOutputStream(java.io.PipedInputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedOutputStream() { throw new RuntimeException("skeleton method"); }
  public synchronized void connect(java.io.PipedInputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
