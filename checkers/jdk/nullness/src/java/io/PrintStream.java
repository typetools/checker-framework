package java.io;

import checkers.nullness.quals.*;

public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
  public PrintStream(java.io.OutputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.io.OutputStream a1, boolean a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.io.OutputStream a1, boolean a2, java.lang.String a3) throws java.io.UnsupportedEncodingException { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.lang.String a1) throws java.io.FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.lang.String a1, java.lang.String a2) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.io.File a1) throws java.io.FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(java.io.File a1, java.lang.String a2) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
  public boolean checkError() { throw new RuntimeException("skeleton method"); }
  public void write(int a1) { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void print(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void print(char a1) { throw new RuntimeException("skeleton method"); }
  public void print(int a1) { throw new RuntimeException("skeleton method"); }
  public void print(long a1) { throw new RuntimeException("skeleton method"); }
  public void print(float a1) { throw new RuntimeException("skeleton method"); }
  public void print(double a1) { throw new RuntimeException("skeleton method"); }
  public void print(char[] a1) { throw new RuntimeException("skeleton method"); }
  public void print(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void print(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public void println() { throw new RuntimeException("skeleton method"); }
  public void println(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void println(char a1) { throw new RuntimeException("skeleton method"); }
  public void println(int a1) { throw new RuntimeException("skeleton method"); }
  public void println(long a1) { throw new RuntimeException("skeleton method"); }
  public void println(float a1) { throw new RuntimeException("skeleton method"); }
  public void println(double a1) { throw new RuntimeException("skeleton method"); }
  public void println(char[] a1) { throw new RuntimeException("skeleton method"); }
  public void println(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public void println(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  // The vararg arrays can actually be null, but let's not annotate them
  // because passing null is bad sytle; see whether this annotation is useful.
  public java.io.PrintStream printf(java.lang.String a1, @Nullable java.lang.Object ... a2) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream printf(@Nullable java.util.Locale a1, java.lang.String a2, @Nullable java.lang.Object... a3) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream format(java.lang.String a1, @Nullable java.lang.Object... a2) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream format(@Nullable java.util.Locale a1, java.lang.String a2, @Nullable java.lang.Object[] a3) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream append(@Nullable java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream append(@Nullable java.lang.CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public java.io.PrintStream append(@Nullable char a1) { throw new RuntimeException("skeleton method"); }
}
