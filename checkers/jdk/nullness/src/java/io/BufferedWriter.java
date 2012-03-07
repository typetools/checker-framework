package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class BufferedWriter extends Writer {
  public BufferedWriter(Writer a1) { throw new RuntimeException("skeleton method"); }
  public BufferedWriter(Writer a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void newLine() throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
