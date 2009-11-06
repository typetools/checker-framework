package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PushbackInputStream extends FilterInputStream {
  public PushbackInputStream(java.io.InputStream a1, int a2) { throw new RuntimeException("skeleton method"); }
  public PushbackInputStream(java.io.InputStream a1) { throw new RuntimeException("skeleton method"); }
  public int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int available() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public long skip(long a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public synchronized void mark(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
