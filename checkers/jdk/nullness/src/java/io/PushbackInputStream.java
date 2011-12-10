package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PushbackInputStream extends FilterInputStream {
  public PushbackInputStream(InputStream a1, int a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public PushbackInputStream(InputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void unread(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void unread(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void unread(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public long skip(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public synchronized void mark(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
