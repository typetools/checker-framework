import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;

class OverriddenMethodsTestParent {
  public void foo(@Sibling1 Object obj, @Sibling2 Object obj2) {}

  public void bar(@Sibling1 OverriddenMethodsTestParent this, @Sibling2 Object obj) {}

  public void barz(@Sibling1 OverriddenMethodsTestParent this, @Sibling2 Object obj) {}

  public void qux(Object obj1, Object obj2) {
    // :: warning: argument
    foo(obj1, obj2);
  }

  public void thud(Object obj1, Object obj2) {
    // :: warning: argument
    foo(obj1, obj2);
  }
}

class OverriddenMethodsTestChild extends OverriddenMethodsTestParent {
  @Override
  public void foo(Object obj, Object obj2) {
    // :: warning: (assignment)
    @Sibling1 Object o = obj;
    // :: warning: (assignment)
    @Sibling2 Object o2 = obj2;
  }

  @Override
  public void bar(Object obj) {
    // :: warning: (assignment)
    @Sibling1 OverriddenMethodsTestChild child = this;
    // :: warning: (assignment)
    @Sibling2 Object o = obj;
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

  public void callqux(@Sibling1 Object obj1, @Sibling2 Object obj2) {
    qux(obj1, obj2);
  }
}
