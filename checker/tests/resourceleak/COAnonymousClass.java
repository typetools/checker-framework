// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class COAnonymousClass {
  @InheritableMustCall("foo")
  static class Foo {

    void foo() {}

    @CreatesMustCallFor("this")
    void resetFoo() {}

    void other() {

      Runnable r =
          new Runnable() {
            @Override
            @CreatesMustCallFor("Foo.this")
            // :: error: (creates.mustcall.for.override.invalid)
            public void run() {
              resetFoo();
            }
          };
      call_run(r);
    }

    void other2() {

      Runnable r =
          new Runnable() {
            @Override
            @CreatesMustCallFor("this")
            // :: error: (creates.mustcall.for.invalid.target)
            // :: error: (creates.mustcall.for.override.invalid)
            public void run() {
              // This error definitely must be issued, since Foo.this != this.
              // :: error: (reset.not.owning)
              resetFoo();
            }
          };
      call_run(r);
    }

    // If this call to run() were permitted with no errors, this would be unsound.
    void call_run(Runnable r) {
      r.run();
    }
  }
}
