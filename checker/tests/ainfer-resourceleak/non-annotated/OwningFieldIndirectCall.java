import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwningFieldIndirectCall {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  static class Utility {
    @EnsuresCalledMethods(value = "#1", methods = "a")
    public static void closeStream(Foo f) {
      if (f != null) {
        f.a();
      }
    }
  }

  static class DisposeFieldUsingECM {
    final Foo f; // expect owning annotation for this field

    DisposeFieldUsingECM() {
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    void dispose() {
      Utility.closeStream(f);
    }
  }

  void testCorrect() {
    DisposeFieldUsingECM d = new DisposeFieldUsingECM();
    d.dispose();
  }
}
