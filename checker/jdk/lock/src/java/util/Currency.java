package java.util;
import org.checkerframework.checker.lock.qual.*;

public final class Currency implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  protected Currency() {}
  public static Currency getInstance(String a1) { throw new RuntimeException("skeleton method"); }
  public static Currency getInstance(Locale a1) { throw new RuntimeException("skeleton method"); }
  public String getCurrencyCode() { throw new RuntimeException("skeleton method"); }
  public String getSymbol() { throw new RuntimeException("skeleton method"); }
  public String getSymbol(Locale a1) { throw new RuntimeException("skeleton method"); }
  public int getDefaultFractionDigits() { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Currency this) { throw new RuntimeException("skeleton method"); }
}
