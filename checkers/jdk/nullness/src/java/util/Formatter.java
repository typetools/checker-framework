package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Formatter{
  public enum BigDecimalLayoutForm {
      SCIENTIFIC, DECIMAL_FLOAT;
  }
  public Formatter() { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable java.lang.Appendable a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(@Nullable java.lang.Appendable a1, @Nullable java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
  public Formatter(java.lang.String a1)throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.lang.String a1, java.lang.String a2)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.lang.String a1, java.lang.String a2, @Nullable java.util.Locale a3)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1)throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1, java.lang.String a2)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.File a1, java.lang.String a2, @Nullable java.util.Locale a3)throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.PrintStream a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1, java.lang.String a2)throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public Formatter(java.io.OutputStream a1, java.lang.String a2, @Nullable java.util.Locale a3)throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.util.Locale locale() { throw new RuntimeException("skeleton method"); }
  public java.lang.Appendable out() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public void flush() { throw new RuntimeException("skeleton method"); }
  public void close() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.IOException ioException() { throw new RuntimeException("skeleton method"); }
  public java.util.Formatter format(java.lang.String a1, java.lang.Object... a2) { throw new RuntimeException("skeleton method"); }
  public java.util.Formatter format(@Nullable java.util.Locale a1, java.lang.String a2, java.lang.Object... a3) { throw new RuntimeException("skeleton method"); }
}
