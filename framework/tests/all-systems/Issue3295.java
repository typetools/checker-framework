import java.util.List;

public class Issue3295 {
  interface P<T> {

    Class<? super T> h();

    List<P<? super T>> g();
  }

  interface Q {}

  @SuppressWarnings("interning:unnecessary.equals") // This warning is expected.
  static void f(P<? extends Q> t) {
    t.g().stream().filter(x -> x.h().equals(Q.class));
  }
}
