public class TypeVars {

  class Test1<T> {
    void m() {
      @SuppressWarnings("unchecked")
      T x = (T) new Object();

      Object o = x;
    }

    class Inner1<X extends T> {}

    public Inner1<T> method1() {
      return new Inner1<>();
    }
  }

  // It's difficult to add more test cases that
  // should work for all type systems.
  // Ensure that for the different type systems, annotations
  // on the type variable are propagated correctly.
}
