import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

// A smaller version of Issue7678.java.
@SuppressWarnings("all")
public class Issue7678Smaller {
  static class MyMap<B, C> {
    static <A, B2, C2> Collector<A, ?, MyMap<B2, C2>> toMyMap(Function<? super A, C2> f) {
      return null;
    }
  }

  static class BiStream<H> {
    static <F, H2> Collector<F, ?, BiStream<H2>> toBiStream(Collector<F, ?, H2> valueCollector) {
      return null;
    }

    <V2> V2 mapValues(Function<H, V2> f) {
      return null;
    }
  }

  void test(Stream<String> items, Function<String, String> stringStringFunction) {
    items
        .collect(BiStream.toBiStream(MyMap.toMyMap(stringStringFunction)))
        .mapValues(byLen -> byLen);
  }
}
