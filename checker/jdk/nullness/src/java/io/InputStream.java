package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public abstract class InputStream implements Closeable {
  public InputStream() { throw new RuntimeException("skeleton method"); }
  public abstract int read() throws IOException;
  public int read(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public long skip(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void mark(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
}
