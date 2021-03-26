// Test case for issue 953
// https://github.com/typetools/checker-framework/issues/953
// The signture of MyStream#collect is slightly different than the one in Issue953.java

import java.util.List;

public class Issue953b {
  class MyCollector<A, B, C> {}

  class MyStream<E> {
    <F, G> F collect(MyCollector<? extends E, G, F> param) {
      throw new RuntimeException();
    }
  }

  public static void test(MyStream<Integer> y) {
    // Type argument inference fails, so a checker may report a type checking error.
    @SuppressWarnings("all")
    List<Integer> counts = y.collect(toList());
  }

  static <H> MyCollector<H, ?, List<H>> toList() {
    throw new RuntimeException();
  }
}
