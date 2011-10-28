package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedInputStream extends InputStream {
  public PipedInputStream(PipedOutputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public PipedInputStream(PipedOutputStream a1, int a2) throws IOException { throw new RuntimeException("skeleton method"); }
  public PipedInputStream() { throw new RuntimeException("skeleton method"); }
  public PipedInputStream(int a1) { throw new RuntimeException("skeleton method"); }
  public void connect(PipedOutputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
