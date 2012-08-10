package java.io;

import checkers.nonnull.quals.Nullable;

@checkers.quals.DefaultQualifier(checkers.nonnull.quals.NonNull.class)

public class BufferedOutputStream extends FilterOutputStream {
  public BufferedOutputStream(OutputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public BufferedOutputStream(OutputStream a1, int a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public synchronized void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void flush() throws IOException { throw new RuntimeException("skeleton method"); }
}
