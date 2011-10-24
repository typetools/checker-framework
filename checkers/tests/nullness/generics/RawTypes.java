import checkers.nullness.quals.*;

class RawTypes {
  void m() throws ClassNotFoundException {
    // The upper bound of the Class type variable is Nullable.
    // The upper bound of the raw type of c is defaulted to NonNull.
    //:: error: (assignment.type.incompatible)
    Class c = Class.forName("bla");
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
}
