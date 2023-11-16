// This test corrects the wrong @MustCallAlias annotation written on the constrcutor when there are
// more than one owning field.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ReplaceMustCallAliasAnnotation {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("a")
  private class TwoOwningFields {

    final @Owning Foo f1;
    final @Owning Foo f2;

    @SuppressWarnings({"assignment", "mustcallalias.out.of.scope"})
    @MustCallAlias TwoOwningFields(@MustCallAlias Foo foo1, Foo foo2) {
      f1 = foo1;
      f2 = foo2;
    }

    @EnsuresCalledMethods(
        value = {"this.f1", "this.f2"},
        methods = {"a"})
    public void a() {
      f1.a();
      f2.a();
    }
  }

  void testOwningAnnotations() {
    Foo f1 = new Foo();
    // :: warning: (required.method.not.called)
    Foo f2 = new Foo();
    TwoOwningFields t = new TwoOwningFields(f1, f2);
    t.a();
  }
}
