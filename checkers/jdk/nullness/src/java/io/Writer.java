package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class Writer implements Appendable, Closeable, Flushable {
  protected Writer() {}
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(char[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public abstract void write(char[] a1, int a2, int a3) throws IOException;
  public void write(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(String a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public Writer append(@Nullable CharSequence a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public Writer append(@Nullable CharSequence a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public Writer append(char a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public abstract void flush() throws IOException;
  public abstract void close() throws IOException;
}
