import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class OverriddenMethodsTest {
  static class OverriddenMethodsTestParent {
    public void foo(@AinferSibling1 Object obj, @AinferSibling2 Object obj2) {}

    public void bar(@AinferSibling1 OverriddenMethodsTestParent this, @AinferSibling2 Object obj) {}

    public void barz(
        @AinferSibling1 OverriddenMethodsTestParent this, @AinferSibling2 Object obj) {}

    public void qux(Object obj1, Object obj2) {
      // :: warning: (argument)
      foo(obj1, obj2);
    }

    public void thud(Object obj1, Object obj2) {
      // :: warning: (argument)
      foo(obj1, obj2);
    }
  }

  class OverriddenMethodsTestChild extends OverriddenMethodsTestParent {
    @Override
    public void foo(Object obj, Object obj2) {
      // :: warning: (assignment)
      @AinferSibling1 Object o = obj;
      // :: warning: (assignment)
      @AinferSibling2 Object o2 = obj2;
    }

    @Override
    public void bar(Object obj) {
      // :: warning: (assignment)
      @AinferSibling1 OverriddenMethodsTestChild child = this;
      // :: warning: (assignment)
      @AinferSibling2 Object o = obj;
    }

    @SuppressWarnings("all")
    @Override
    public void barz(Object obj) {}

    public void callbarz(Object obj) {
      // If the @SuppressWarnings("all") on the overridden version of barz above is not
      // respected, and the annotations on the receiver and parameter of barz are
      // inferred, then the following call to barz will result in a method.invocation
      // and an argument type checking errors.
      barz(obj);
    }

    public void callqux(@AinferSibling1 Object obj1, @AinferSibling2 Object obj2) {
      qux(obj1, obj2);
    }
  }
}
