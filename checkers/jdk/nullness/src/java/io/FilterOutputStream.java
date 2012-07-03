package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class FilterOutputStream extends OutputStream {
  protected FilterOutputStream() {}
  public FilterOutputStream(@Nullable OutputStream a1) { super(); throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
