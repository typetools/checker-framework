package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PushbackReader extends Reader {
  public PushbackReader(java.io.Reader a1, int a2) { throw new RuntimeException("skeleton method"); }
  public PushbackReader(java.io.Reader a1) { throw new RuntimeException("skeleton method"); }
  public int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void unread(char[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean ready() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void mark(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void reset() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean markSupported() { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public long skip(long a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
