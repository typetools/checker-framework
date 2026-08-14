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

  // Same as test(), but with the lambda parenthesized when passed as an argument. The
  // enclosing-tree lookup used to see the immediate PARENTHESIZED wrapper instead of the
  // enclosing invocation, so the re-entrancy guard never triggered for this case.
  private void testParenthesized(Stream<String> items) {
    Function<String, Integer> length = String::length;
    items.collect(BiStream.toBiStream(MyMap.toMyMap(length, (s -> s)))).mapValues((byLen -> byLen));
  }

  // Same as test(), but with the lambda as a branch of a conditional expression passed as an
  // argument. The enclosing-tree lookup used to see the immediate CONDITIONAL_EXPRESSION
  // wrapper instead of the enclosing invocation, so the re-entrancy guard never triggered for
  // this case.
  private void testConditional(Stream<String> items, boolean cond) {
    Function<String, Integer> length = String::length;
    items
        .collect(BiStream.toBiStream(MyMap.toMyMap(length, s -> s)))
        .mapValues(cond ? byLen -> byLen : byLen2 -> byLen2);
  }

  // Same as test(), but with the lambda as the result of a switch expression passed as an
  // argument. The enclosing-tree lookup used to see the immediate CASE wrapper instead of the
  // enclosing invocation, so the re-entrancy guard never triggered for this case.
  private void testSwitch(Stream<String> items, int cond) {
    Function<String, Integer> length = String::length;
    items
        .collect(BiStream.toBiStream(MyMap.toMyMap(length, s -> s)))
        .mapValues(
  private void testSwitchYield(Stream<String> items, int cond) {
    Function<String, Integer> length = String::length;
    items
        .collect(BiStream.toBiStream(MyMap.toMyMap(length, s -> s)))
        .mapValues(
            switch (cond) {
              case 1 -> {
                yield byLen -> byLen;
              }
              default -> {
                yield byLen2 -> byLen2;
              }
            });
  }
  }
}
