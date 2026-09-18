import java.util.Comparator;
import org.checkerframework.dataflow.qual.Pure;

public class Issue7680 {
  interface Foo {
    // Comparator.comparing and Comparator.thenComparing require a side-effect-free key extractor.
    @Pure
    int x();

    @Pure
    int y();
  }

  <Q extends Foo> Comparator<Q> test() {
    return Comparator.comparing(Q::x).thenComparing(Q::y);
  }
}
