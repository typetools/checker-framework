import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class AssertAfter {

  protected @Nullable String value;

  @EnsuresNonNull("value")
  public boolean setRepNonNull() {
    value = "";
    return true;
  }

  public void plain() {
    // :: error: (dereference.of.nullable)
    value.toString();
  }

  public void testAfter() {
    setRepNonNull();
    value.toString();
  }

  public void testBefore() {
    // :: error: (dereference.of.nullable)
    value.toString();
    setRepNonNull();
  }

  public void withCondition(@Nullable String t) {
    if (t == null) {
      setRepNonNull();
    }
    // :: error: (dereference.of.nullable)
    value.toString();
  }

  public void inConditionInTrue() {
    if (setRepNonNull()) {
      value.toString();
    } else {
      // nothing to do
    }
  }

  // skip-test: Come back when working on improved flow
  public void asCondition() {
    if (setRepNonNull()) {
    } else {
      value.toString(); // valid!
    }
  }
}

// Test that private fields can be mentioned in pre- and post-conditions.

class A {
  private @Nullable String privateField = null;

  @EnsuresNonNull("privateField")
  public void m1() {
    privateField = "hello";
  }

  @RequiresNonNull("privateField")
  public void m2() {}
}

class B {

  void f() {
    A a = new A();
    a.m1();
    a.m2();
  }
}
