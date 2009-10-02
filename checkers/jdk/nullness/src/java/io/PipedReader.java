package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedReader extends Reader {
  public PipedReader(java.io.PipedWriter a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedReader(java.io.PipedWriter a1, int a2) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedReader() { throw new RuntimeException("skeleton method"); }
  public PipedReader(int a1) { throw new RuntimeException("skeleton method"); }
  public void connect(java.io.PipedWriter a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized boolean ready() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
