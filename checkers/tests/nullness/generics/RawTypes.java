import checkers.nullness.quals.*;

//@skip-test
class RawTypes {
  void m() throws ClassNotFoundException {
    // TODO: this fails but shouldn't
    Class c1 = Class.forName("bla");
    Class<?> c2 = Class.forName("bla");
  }

  class Test<X extends Number> {}
  
  void bar() {
    // Java will complain about this:
    // Test x = new Test<Object>();

    // ok
    Test y = new Test<Integer>();

    //:: error: (assignment.type.incompatible) :: error: (generic.argument.invalid)
    Test z = new Test<@Nullable Integer>();
  }

  void m(java.lang.reflect.Constructor<?> c) {
    // TODO: this fails but shouldn't
    Class cls1 = c.getParameterTypes()[0];
    Class<?> cls2 = c.getParameterTypes()[0];
  }
}
