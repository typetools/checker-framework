package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

@Deprecated
public class StringBufferInputStream extends InputStream {
  public StringBufferInputStream(String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int read() { throw new RuntimeException("skeleton method"); }
  public synchronized int read(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized long skip(long a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int available() { throw new RuntimeException("skeleton method"); }
  public synchronized void reset() { throw new RuntimeException("skeleton method"); }
}
