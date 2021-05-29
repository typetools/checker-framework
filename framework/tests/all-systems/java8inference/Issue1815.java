// Test case for Issue 1815:
// https://github.com/typetools/checker-framework/issues/1815

import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("all") // just check for crashes
abstract class Issue1815 {

  class A extends B {}

  static class B<One extends B<One, Two>, Two extends B.I<One, Two>> {
    static class I<Three extends B<Three, Four>, Four extends I<Three, Four>> {}
  }

  abstract A f(Integer x);

  void test(List<Integer> xs) {
    Stream.of(xs.stream().map(x -> f(x)), xs.stream().map(x -> f(x))).flatMap(stream -> stream);
  }
}
