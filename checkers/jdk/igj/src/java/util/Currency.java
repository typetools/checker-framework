package java.util;
import checkers.igj.quals.*;

@Immutable
public final class Currency implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  protected Currency(@ReadOnly Currency this) {}
  public static Currency getInstance(String a1) { throw new RuntimeException("skeleton method"); }
  public static Currency getInstance(Locale a1) { throw new RuntimeException("skeleton method"); }
  public String getCurrencyCode() { throw new RuntimeException("skeleton method"); }
  public String getSymbol() { throw new RuntimeException("skeleton method"); }
  public String getSymbol(Locale a1) { throw new RuntimeException("skeleton method"); }
  public int getDefaultFractionDigits() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
