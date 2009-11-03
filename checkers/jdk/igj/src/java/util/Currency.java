package java.util;
import checkers.igj.quals.*;

@Immutable
public final class Currency implements java.io.Serializable {
  protected Currency() @ReadOnly {}
  public static java.util.Currency getInstance(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.util.Currency getInstance(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String getCurrencyCode() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getSymbol() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getSymbol(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public int getDefaultFractionDigits() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
