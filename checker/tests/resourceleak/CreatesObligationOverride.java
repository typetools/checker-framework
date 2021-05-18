// A test case that a create-obligation method cannot be called via dynamic dispatch
// without resetting the obligation.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("a") class CreatesObligationOverride {
  @CreatesObligation
  @Override
  // :: error: creates.obligation.override.invalid
  public String toString() {
    return "this method could re-assign a field or do something else it shouldn't";
  }

  public void a() {}

  public static void test_no_cast() {
    // :: error: required.method.not.called
    CreatesObligationOverride co = new CreatesObligationOverride();
    co.a();
    co.toString();
  }

  public static void test_cast() {
    // it would be ideal if the checker issued an error directly here, but the best we can do is
    // issue the error above when the offending version of toString() is defined
    CreatesObligationOverride co = new CreatesObligationOverride();
    co.a();
    Object o = co;
    o.toString();
  }
}
