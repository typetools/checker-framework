package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class CharArrayReader extends Reader {
  public CharArrayReader(char[] a1) { throw new RuntimeException("skeleton method"); }
  public CharArrayReader(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public long skip(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean ready() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public void mark(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
}
