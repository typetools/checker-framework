import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

// Related to lambda parameters.
@SuppressWarnings("all")
public class Issue7678 {
  static class MyMap<Z, Y> {
    static <A, B, C> Collector<A, ?, MyMap<B, C>> toMyMap(
        Function<? super A, ? extends B> keyFunction,
        Function<? super A, ? extends C> valueFunction) {
      return null;
    }
  }

  static class BiStream<I, E> {
    static <F, G, H> Collector<F, ?, BiStream<G, H>> toBiStream(
        Collector<? super F, ?, H> valueCollector) {
      return null;
    }

    <V2> BiStream<I, V2> mapValues(Function<? super E, ? extends V2> valueMapper) {
      return null;
    }
  }

  private void test(Stream<String> items) {
    Function<String, Integer> length = String::length;
    items.collect(BiStream.toBiStream(MyMap.toMyMap(length, s -> s))).mapValues(byLen -> byLen);
  }
}
