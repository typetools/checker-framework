import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasParams {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("a")
  private class MCAConstructor {

    final @Owning Foo f; // expect owning annotation for this field

    // The Must Call Checker for assigning @MustCallAlias parameters to @Owning fields reports a
    // false positive.
    @SuppressWarnings("assignment")
    MCAConstructor(Foo foo) {
      f = foo;
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    public void a() {
      f.a();
    }
  }

  void testMCAConstructor() {
    // :: warning: (required.method.not.called)
    Foo f = new Foo();
    MCAConstructor mcac = new MCAConstructor(f);
    mcac.a();
  }

  @InheritableMustCall("a")
  private class MCASuperClass {
    int i;
    final @Owning Foo f;

    // The Must Call Checker for assigning @MustCallAlias parameters to @Owning fields reports a
    // false positive.
    @SuppressWarnings("assignment")
    @MustCallAlias MCASuperClass(@MustCallAlias Foo foo, int i) {
      f = foo;
      i = i;
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    public void a() {
      f.a();
    }
  }

  private class MCASuperCall extends MCASuperClass {
    MCASuperCall(Foo foo) {
      super(foo, 1);
    }
  }

  void mCASuperCallTest() {
    // :: warning: (required.method.not.called)
    Foo f = new Foo();
    MCASuperCall mcaSuperCall = new MCASuperCall(f);
    mcaSuperCall.a();
  }

  private class MCAThisCall extends MCASuperClass {
    @MustCallAlias MCAThisCall(@MustCallAlias Foo foo) {
      super(foo, 1);
    }

    MCAThisCall(Foo foo, boolean b) {
      this(foo);
    }
  }

  void mCAThisCallTest() {
    // :: warning: (required.method.not.called)
    Foo f = new Foo();
    MCAThisCall mcaThisCall = new MCAThisCall(f, true);
    mcaThisCall.a();
  }

  private class MCAMethod {
    Foo returnFoo(Foo foo) {
      return foo;
    }

    void returnFooTest() {
      // :: warning: (required.method.not.called)
      Foo f = new Foo();
      Foo foo = returnFoo(f);
      foo.a();
    }

    Foo returnAliasFoo(Foo foo) {
      Foo f = foo;
      return f;
    }

    MCASuperClass returnAliasFoo2(Foo foo, int i) {
      MCASuperClass f = new MCASuperClass(foo, i);
      return f;
    }

    void testReturnAliasFoo2() {
      // :: warning: (required.method.not.called)
      Foo foo = new Foo();
      MCASuperClass f = returnAliasFoo2(foo, 0);
      f.a();
    }

    void returnAliasFooTest() {
      // :: warning: (required.method.not.called)
      Foo f = new Foo();
      Foo foo = returnAliasFoo(f);
      foo.a();
    }

    Foo returnFooAllPaths(Foo foo) {
      if (true) {
        Foo f = foo;
        return f;
      } else {
        return foo;
      }
    }

    void returnFooAllPathsTest() {
      // :: warning: (required.method.not.called)
      Foo f = new Foo();
      Foo foo = returnFooAllPaths(f);
      foo.a();
    }

    Foo returnFooSomePath(Foo foo) {
      if (true) {
        Foo f = new Foo();
        return f;
      } else {
        return foo;
      }
    }

    void returnFooSomePathTest() {
      Foo f = new Foo();
      Foo foo = returnFooSomePath(f);
      f.a();
      foo.a();
    }
  }
}
