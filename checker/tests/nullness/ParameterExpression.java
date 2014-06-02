import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.*;

public class ParameterExpression {
  public void m1(@Nullable Object o, @Nullable Object o1, @Nullable Object o2, @Nullable Object o3) {
    m2(o);
    //:: error: (dereference.of.nullable)
    o.toString();
    m3(o);
    o.toString();
    m4(o1, o2, o3);
    //:: error: (dereference.of.nullable)
    o1.toString();
    //:: error: (dereference.of.nullable)
    o2.toString();
    o3.toString();
  }

  @SuppressWarnings("assert.postcondition.not.satisfied")
  // "#0" is illegal syntax; it should be "#1"
  @EnsuresNonNull("#0")
  //:: error: (flowexpr.parse.error)
  public void m2(final @Nullable Object o) {
  }

  @SuppressWarnings("contracts.postcondition.not.satisfied")
  @EnsuresNonNull("#1")
  public void m3(final @Nullable Object o) {
  }

  @SuppressWarnings("contracts.postcondition.not.satisfied")
  @EnsuresNonNull("#3")
  public void m4(@Nullable Object x1, @Nullable Object x2, final @Nullable Object x3) {
  }

}
