package java.io;

import checkers.nullness.quals.*;

public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
  public PrintStream(OutputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(OutputStream a1, boolean a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(OutputStream a1, boolean a2, String a3) throws UnsupportedEncodingException { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(String a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(String a1, String a2) throws FileNotFoundException, UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(File a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(File a1, String a2) throws FileNotFoundException, UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
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
  public void print(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public void print(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public void println() { throw new RuntimeException("skeleton method"); }
  public void println(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void println(char a1) { throw new RuntimeException("skeleton method"); }
  public void println(int a1) { throw new RuntimeException("skeleton method"); }
  public void println(long a1) { throw new RuntimeException("skeleton method"); }
  public void println(float a1) { throw new RuntimeException("skeleton method"); }
  public void println(double a1) { throw new RuntimeException("skeleton method"); }
  public void println(char[] a1) { throw new RuntimeException("skeleton method"); }
  public void println(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public void println(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  // The vararg arrays can actually be null, but let's not annotate them
  // because passing null is bad sytle; see whether this annotation is useful.
  public PrintStream printf(String a1, @Nullable Object ... a2) { throw new RuntimeException("skeleton method"); }
  public PrintStream printf(@Nullable java.util.Locale a1, String a2, @Nullable Object... a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream format(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
  public PrintStream format(@Nullable java.util.Locale a1, String a2, @Nullable Object... a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(@Nullable CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(@Nullable CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(char a1) { throw new RuntimeException("skeleton method"); }
}
