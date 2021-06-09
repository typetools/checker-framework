// Test case for https://github.com/kelloggm/object-construction-checker/issues/368

import org.checkerframework.checker.mustcall.qual.*;

class CreatesMustCallForInnerClass {
  static class Foo {

    @CreatesMustCallFor("this")
    void resetFoo() {}

    /** non-static inner class */
    class Bar {
      @CreatesMustCallFor
      void bar() {
        // :: error: reset.not.owning
        resetFoo();
      }
    }

    void callBar() {
      Bar b = new Bar();
      // If this call to bar() were permitted with no errors, this would be unsound.
      b.bar();
    }
  }
}
