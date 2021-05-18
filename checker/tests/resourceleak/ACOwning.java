import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACOwning {

  @MustCall("a") static class Foo {
    void a() {}
  }

  Foo makeFoo() {
    return new Foo();
  }

  static void takeOwnership(@Owning Foo foo, Foo f) {
    foo.a();
  }

  static void noOwnership(Foo foo) {}

  // :: error: required.method.not.called
  static void takeOwnershipWrong(@Owning Foo foo) {}

  static @NotOwning Foo getNonOwningFoo() {
    // :: error: required.method.not.called
    return new Foo();
  }

  static void callGetNonOwningFoo() {
    getNonOwningFoo();
  }

  static void ownershipInCallee() {
    Foo f = new Foo();
    // :: error: required.method.not.called
    takeOwnership(f, new Foo());
    // :: error: required.method.not.called
    Foo g = new Foo();
    noOwnership(g);
  }

  // make sure enum doesn't crash things
  static enum TestEnum {
    CASE1,
    CASE2,
    CASE3
  }

  @Owning
  public Foo owningAtReturn() {
    return new Foo();
  }

  void owningAtReturnTest() {
    // :: error: required.method.not.called
    Foo f = owningAtReturn();
  }

  void ownershipTest() {
    // :: error: required.method.not.called
    takeOwnership(new Foo(), makeFoo());
  }

  @MustCall({})
  // :: error: super.invocation.invalid
  private class SubFoo extends Foo {

    void test() {
      SubFoo f = new SubFoo();
    }

    void test2() {
      // :: error: required.method.not.called
      Foo f = new Foo();
    }

    void test3() {
      Foo f = new SubFoo();
    }
  }
}
