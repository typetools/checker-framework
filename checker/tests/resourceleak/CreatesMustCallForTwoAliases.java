// Test case for taking the least upper bound of obligations.

import org.checkerframework.checker.mustcall.qual.*;

public class CreatesMustCallForTwoAliases {
  @InheritableMustCall("a")
  static class Foo {

    @SuppressWarnings("mustcall")
    @MustCall() Foo() {
      // unconnected socket like
    }

    @CreatesMustCallFor("this")
    void reset() {}

    void a() {}
  }

  public static void test1() {
    Foo a = new Foo();
    // :: error: required.method.not.called
    Foo b = a;
    b.reset();
  }

  @CreatesMustCallFor("#1")
  public static void sneakyReset(Foo f) {
    f.reset();
  }

  public static void test2() {
    Foo a = new Foo();
    // :: error: required.method.not.called
    Foo b = a;
    sneakyReset(b);
  }

  public static void test3(Foo b) {
    Foo a = new Foo();
    // :: error: required.method.not.called
    b = a;
    sneakyReset(b);
  }

  public static void test4(Foo b) {
    // :: error: required.method.not.called
    Foo a = new Foo();
    b = a;
    sneakyReset(a);
  }
}
