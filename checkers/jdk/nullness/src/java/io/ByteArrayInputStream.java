package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ByteArrayInputStream extends InputStream {
  public ByteArrayInputStream(byte[] a1) { throw new RuntimeException("skeleton method"); }
  public ByteArrayInputStream(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized int read() { throw new RuntimeException("skeleton method"); }
  public synchronized int read(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized long skip(long a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int available() { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public void mark(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
