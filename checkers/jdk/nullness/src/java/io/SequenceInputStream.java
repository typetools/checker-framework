package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class SequenceInputStream extends InputStream {
  public SequenceInputStream(java.util.Enumeration<? extends java.io.InputStream> a1) { throw new RuntimeException("skeleton method"); }
  public SequenceInputStream(java.io.InputStream a1, java.io.InputStream a2) { throw new RuntimeException("skeleton method"); }
  public int available() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
