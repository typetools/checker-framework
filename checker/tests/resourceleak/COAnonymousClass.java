// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class COAnonymousClass {
  static class Foo {

    @CreatesMustCallFor("this")
    void resetFoo() {}

    void other() {

      Runnable r =
          new Runnable() {
            @Override
            @CreatesMustCallFor("Foo.this")
            // :: error: creates.mustcall.for.override.invalid
            public void run() {
              // Ideally, we would not issue the following error. However, the Checker Framework's
              // JavaExpression support
              // (https://checkerframework.org/manual/#java-expressions-as-arguments)
              // treats all versions of "this" (including "Foo.this") as referring to the object
              // that directly contains the annotation, so we treat this call to resetFoo as not
              // permitted.
              // :: error: reset.not.owning
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
            // :: error: creates.mustcall.for.override.invalid
            public void run() {
              // This error definitely must be issued, since Foo.this != this.
              // :: error: reset.not.owning
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
