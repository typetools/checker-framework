// Test case for https://github.com/typetools/checker-framework/issues/2429

import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue2429 {
  interface Transform<S, T> {
    @PolyNull T transform(@PolyNull S in);
  }

  abstract static class Demo {
    abstract <A, B> Function<A, B> map(Function<? super A, ? extends B> f);

    <C> C then(C in) {
      return in;
    }

    void use(Transform<Object, String> tf) {
      then(map(x -> tf.transform(x)));
      then(map(tf::transform));
    }
  }
}
