package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedInputStream extends InputStream {
  public PipedInputStream(java.io.PipedOutputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedInputStream(java.io.PipedOutputStream a1, int a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedInputStream() { throw new RuntimeException("skeleton method"); }
  public PipedInputStream(int a1) { throw new RuntimeException("skeleton method"); }
  public void connect(java.io.PipedOutputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int available() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
