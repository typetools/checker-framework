public class GenericArg<X> {

  void mp(X p) {
    Object l;
  }

  X mr() {
    Object r;
    return null;
  }

  <Y extends Number> void foo(Y p) {
    Object k;
  }

  <Z extends Integer> Z bar() {
    Integer j;
    return null;
  }

  class Tricky {
    void argh(X p) {
      Object a;
    }
  }
}
