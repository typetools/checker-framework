// This test checks that 1) writing a CO annotation on an overridden method that's also
// CO is permitted, and 2) that all overridden versions of a CO method are always also CO.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CreatesMustCallForOverride2 {

  @InheritableMustCall("a")
  static class Foo {

    @CreatesMustCallFor
    public void b() {}

    public void a() {}
  }

  static class Bar extends Foo {

    @Override
    @CreatesMustCallFor
    public void b() {}
  }

  static class Baz extends Foo {}

  static class Qux extends Foo {
    @Override
    public void b() {}
  }

  static class Razz extends Foo {

    public @Owning Foo myFoo;

    @Override
    @EnsuresCalledMethods(value = "this.myFoo", methods = "a")
    public void a() {
      super.a();
      myFoo.a();
    }

    // this version isn't permitted, since it adds a new obligation
    @Override
    @CreatesMustCallFor("this.myFoo")
    // :: error: creates.mustcall.for.override.invalid
    public void b() {}
  }

  static class Thud extends Foo {

    public @Owning Foo myFoo;

    @Override
    @EnsuresCalledMethods(value = "this.myFoo", methods = "a")
    public void a() {
      super.a();
      myFoo.a();
    }

    // this method isn't permitted, since it's also adding a new obligation
    @Override
    @CreatesMustCallFor("this.myFoo")
    @CreatesMustCallFor("this")
    // :: error: creates.mustcall.for.override.invalid
    public void b() {}
  }

  static class Thudless extends Thud {
    // this method override is also NOT permitted, because the @CreatesMustCallFor("this.myFoo")
    // annotation
    // from Thud is inherited!
    @Override
    @CreatesMustCallFor("this")
    // :: error: creates.mustcall.for.override.invalid
    public void b() {}
  }

  static void test1() {
    // :: error: required.method.not.called
    Foo foo = new Foo();
    foo.a();
    foo.b();
  }

  static void test2() {
    // :: error: required.method.not.called
    Foo foo = new Bar();
    foo.a();
    foo.b();
  }

  static void test3() {
    // :: error: required.method.not.called
    Foo foo = new Baz();
    foo.a();
    foo.b();
  }

  static void test4() {
    // :: error: required.method.not.called
    Foo foo = new Qux();
    foo.a();
    foo.b();
  }

  static void test5() {
    // :: error: required.method.not.called
    Bar foo = new Bar();
    foo.a();
    foo.b();
  }

  static void test6() {
    // :: error: required.method.not.called
    Baz foo = new Baz();
    foo.a();
    foo.b();
  }

  static void test7() {
    // :: error: required.method.not.called
    Qux foo = new Qux();
    foo.a();
    foo.b();
  }

  static void test8() {
    // :: error: required.method.not.called
    Foo foo = new Razz();
    foo.a();
    foo.b();
  }

  static void test9() {
    // No error is issued here, because Razz#b is *only* @CreatesMustCallFor("this.myFoo"), not
    // @CreatesMustCallFor("this"). An error is issued at the declaration of Razz#b instead.
    Razz foo = new Razz();
    foo.a();
    foo.b();
  }

  static void test10() {
    // :: error: required.method.not.called
    Foo foo = new Thud();
    foo.a();
    foo.b();
  }

  static void test11() {
    // :: error: required.method.not.called
    Thud foo = new Thud();
    foo.a();
    foo.b();
  }

  static void test12() {
    // :: error: required.method.not.called
    Foo foo = new Thudless();
    foo.a();
    foo.b();
  }

  static void test13() {
    // :: error: required.method.not.called
    Thud foo = new Thudless();
    foo.a();
    foo.b();
  }

  static void test14() {
    // :: error: required.method.not.called
    Thudless foo = new Thudless();
    foo.a();
    foo.b();
  }
}
