import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ClassWithTwoOwningFieldsTest {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("close")
  private class ClassWithTwoOwningFields {
    // :: warning: (required.method.not.called)
    final @Owning Foo foo1;
    // :: warning: (required.method.not.called)
    final @Owning Foo foo2;

    public ClassWithTwoOwningFields(Foo f1, Foo f2) {
      foo1 = f1;
      foo2 = f2;
    }

    void close() {
      foo1.a();
      foo2.a();
    }
  }

  void testTwoOwning() {
    // :: warning: (required.method.not.called)
    Foo f1 = new Foo();
    // :: warning: (required.method.not.called)
    Foo f2 = new Foo();

    ClassWithTwoOwningFields ff = new ClassWithTwoOwningFields(f1, f2);
    ff.close();
  }
}
