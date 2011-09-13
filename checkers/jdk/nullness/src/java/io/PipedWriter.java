package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedWriter extends Writer {
  public PipedWriter(PipedReader a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public PipedWriter() { throw new RuntimeException("skeleton method"); }
  public synchronized void connect(PipedReader a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
