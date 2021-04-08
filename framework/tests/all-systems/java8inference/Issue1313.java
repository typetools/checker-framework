// Test case for Issue 1313.
// https://github.com/typetools/checker-framework/issues/1313

import java.util.stream.Collector;
import java.util.stream.Stream;

interface MyList1313<E> extends Iterable<E> {}

@SuppressWarnings({"all", "type.inference.not.same"}) // check for crashes
public class Issue1313 {
  Stream<?> s;
  Iterable<?> i = s.collect(toMyList1313());

  <F> Collector<F, ?, MyList1313<F>> toMyList1313() {
    return null;
  }
}
