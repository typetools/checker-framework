package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public abstract class FilterWriter extends Writer {
  protected FilterWriter() {}
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
}
