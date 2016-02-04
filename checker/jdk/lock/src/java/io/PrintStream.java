package java.io;

import org.checkerframework.checker.lock.qual.*;

// TODO: Should parameters be @GuardSatisfied, or is the default of @GuardedBy({}) appropriate? (@GuardedBy({}) is more conservative.)
public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
  public PrintStream(OutputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(OutputStream a1, boolean a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(OutputStream a1, boolean a2, String a3) throws UnsupportedEncodingException { super(a1); throw new RuntimeException("skeleton method"); }
  public PrintStream(String a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(String a1, String a2) throws FileNotFoundException, UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(File a1) throws FileNotFoundException { super(null); throw new RuntimeException("skeleton method"); }
  public PrintStream(File a1, String a2) throws FileNotFoundException, UnsupportedEncodingException { super(null); throw new RuntimeException("skeleton method"); }
  public void flush(@GuardSatisfied PrintStream this) { throw new RuntimeException("skeleton method"); }
  public void close(@GuardSatisfied PrintStream this) { throw new RuntimeException("skeleton method"); }
  public boolean checkError(@GuardSatisfied PrintStream this) { throw new RuntimeException("skeleton method"); }
  public void write(@GuardSatisfied PrintStream this, int a1) { throw new RuntimeException("skeleton method"); }
  public void write(@GuardSatisfied PrintStream this, byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, boolean a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, char a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, int a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, long a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, float a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, double a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, char[] a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, String a1) { throw new RuntimeException("skeleton method"); }
  public void print(@GuardSatisfied PrintStream this, Object a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, boolean a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, char a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, int a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, long a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, float a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, double a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, char[] a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, String a1) { throw new RuntimeException("skeleton method"); }
  public void println(@GuardSatisfied PrintStream this, Object a1) { throw new RuntimeException("skeleton method"); }
  // The vararg arrays can actually be null, but let's not annotate them
  // because passing null is bad sytle; see whether this annotation is useful.
  public PrintStream printf(@GuardSatisfied PrintStream this, String a1, Object ... a2) { throw new RuntimeException("skeleton method"); }
  public PrintStream printf(@GuardSatisfied PrintStream this, java.util. Locale a1, String a2, Object... a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream format(@GuardSatisfied PrintStream this, String a1, Object... a2) { throw new RuntimeException("skeleton method"); }
  public PrintStream format(@GuardSatisfied PrintStream this, java.util. Locale a1, String a2, Object... a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(PrintStream this, CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(PrintStream this, CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public PrintStream append(PrintStream this, char a1) { throw new RuntimeException("skeleton method"); }
}
