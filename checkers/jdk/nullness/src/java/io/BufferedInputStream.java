package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class BufferedInputStream extends FilterInputStream {
  public BufferedInputStream(InputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public BufferedInputStream(InputStream a1, int a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public synchronized int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized long skip(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void mark(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
