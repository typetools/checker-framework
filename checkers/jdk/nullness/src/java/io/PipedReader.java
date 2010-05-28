package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class PipedReader extends Reader {
  public PipedReader(PipedWriter a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public PipedReader(PipedWriter a1, int a2) throws IOException { throw new RuntimeException("skeleton method"); }
  public PipedReader() { throw new RuntimeException("skeleton method"); }
  public PipedReader(int a1) { throw new RuntimeException("skeleton method"); }
  public void connect(PipedWriter a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized int read(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized boolean ready() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
