package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedWriter extends Writer {
  public PipedWriter(java.io.PipedReader a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public PipedWriter() { throw new RuntimeException("skeleton method"); }
  public synchronized void connect(java.io.PipedReader a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
