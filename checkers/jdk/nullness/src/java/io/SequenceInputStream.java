package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class SequenceInputStream extends InputStream {
  public SequenceInputStream(java.util.Enumeration<? extends InputStream> a1) { throw new RuntimeException("skeleton method"); }
  public SequenceInputStream(InputStream a1, InputStream a2) { throw new RuntimeException("skeleton method"); }
  public int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
