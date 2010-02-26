package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class FilterWriter extends Writer {
  protected FilterWriter() {}
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
