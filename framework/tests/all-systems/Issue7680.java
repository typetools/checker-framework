import java.util.Comparator;

public class Issue7680 {
  interface Foo {
    int x();

    int y();
  }

  <Q extends Foo> Comparator<Q> test() {
    return Comparator.comparing(Q::x).thenComparing(Q::y);
  }
}
