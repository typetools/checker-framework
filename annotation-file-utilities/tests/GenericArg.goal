public class GenericArg<X> {

  void mp(X p) {
    @X
    Object l;
  }

  X mr() {
    @X
    Object r;
    return null;
  }

  <Y extends Number> void foo(Y p) {
    @X
    Object k;
  }

  <Z extends Integer> Z bar() {
    @X
    Integer j;
    return null;
  }

  class Tricky {
    void argh(X p) {
      @X
      Object a;
    }
  }
}
