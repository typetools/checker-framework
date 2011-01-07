package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Formatter implements java.io.Closeable, java.io.Flushable {
  public enum BigDecimalLayoutForm {
      SCIENTIFIC, DECIMAL_FLOAT;
  }
  public Formatter() { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable Appendable a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable Locale a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable Appendable a1, @Nullable Locale a2) { throw new RuntimeException("skeleton method"); }
  public Formatter(String a1)throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Formatter(String a1, String a2)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(String a1, String a2, @Nullable Locale a3)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1)throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1, String a2)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1, String a2, @Nullable Locale a3)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1, String a2)throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1, String a2, @Nullable Locale a3)throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public @Nullable Locale locale() { throw new RuntimeException("skeleton method"); }
  public Appendable out() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.IOException ioException() { throw new RuntimeException("skeleton method"); }
  public Formatter format(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
  public Formatter format(@Nullable Locale a1, String a2, @Nullable Object... a3) { throw new RuntimeException("skeleton method"); }
}
