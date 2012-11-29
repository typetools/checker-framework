import checkers.nullness.quals.*;

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
  //:: warning: (one.param.index.nullness.parse.error)
  @AssertNonNullAfter("#0")
  public void m2(@Nullable Object o) {
  }

  @SuppressWarnings("assert.postcondition.not.satisfied")
  @AssertNonNullAfter("#1")
  public void m3(@Nullable Object o) {
  }

  @SuppressWarnings("assert.postcondition.not.satisfied")
  @AssertNonNullAfter("#3")
  public void m4(@Nullable Object x1, @Nullable Object x2, @Nullable Object x3) {
  }

  @SuppressWarnings("assert.postcondition.not.satisfied")
  //:: warning: (zero.param.index.nullness.parse.error)
  @AssertNonNullAfter("#0")
  public void m5() {
  }

  @SuppressWarnings("assert.postcondition.not.satisfied")
  //:: warning: (param.index.nullness.parse.error)
  @AssertNonNullAfter("#0")
  public void m6(@Nullable Object o, @Nullable Object p) {
  }

}
