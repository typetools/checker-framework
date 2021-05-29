// Test case for Issue 1308.
// https://github.com/typetools/checker-framework/issues/1308

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

class Map1308<K, V> {}

@SuppressWarnings("all") // check for crashes
public class Issue1308 {
  void bar(Stream<Number> stream) {
    new Inner(stream.collect(transform(data -> convert(data), Function.identity())));
  }

  String convert(Number entry) {
    return "";
  }

  class Inner {
    Inner(Map1308<String, Number> data) {}
  }

  static <T, K, V> Collector<T, ?, Map1308<K, V>> transform(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    return null;
  }
}
