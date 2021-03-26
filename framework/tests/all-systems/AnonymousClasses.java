import java.util.Comparator;
import java.util.Date;

// Checkers may issue type checking errors for this class, but they should not crash
@SuppressWarnings("all")
public class AnonymousClasses {
  // test anonymous classes
  private void testAnonymous() {
    Foo x = new Foo() {};
    new Object() {
      public boolean equals(Object o) {
        return true;
      }
    }.equals(null);

    Date d = new Date() {};
  }

  private <T extends Comparator<T>> void testGenericAnonymous() {
    Gen<T> g = new Gen<T>() {};
    GenInter<T> gi = new GenInter<T>() {};
  }

  class Gen<F extends Object> {
    public Gen() {}
  }

  interface GenInter<E> {}

  interface Foo {}
}
