// Test case for https://github.com/typetools/checker-framework/issues/5911

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MultipleOwnedResourcesOfDifferentTypes {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("b")
  static class Bar {
    void b() {}
  }

  @InheritableMustCall("finalizer")
  static class OwningField {

    private final @Owning Foo owningFoo;

    private final @Owning Bar owningBar;

    public OwningField() {
      this.owningFoo = new Foo();
      this.owningBar = new Bar();
    }

    @EnsuresCalledMethods(
        value = {"owningBar"},
        methods = {"b"})
    @EnsuresCalledMethods(
        value = {"this.owningFoo"},
        methods = {"a"})
    void finalizer() {
      try {
        this.owningFoo.a();
      } finally {
        this.owningBar.b();
      }
    }
  }
}
