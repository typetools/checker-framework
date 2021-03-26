// Test case for Issue 1332.
// https://github.com/typetools/checker-framework/issues/1332

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("all") // check for crashes
abstract class Issue1332 {
  void foo(List<Long> ll) {
    Function<String, Long> test =
        s -> {
          long result = getOnlyElement(ll.stream().collect(Collectors.toSet()));
          return result;
        };
  }

  abstract <T> T getOnlyElement(Iterable<T> iterable);

  private void test1() {
    byte[][] byteArrayArray = new byte[][] {};
    Stream<byte[]> stream = Stream.of(byteArrayArray);
  }

  private void test2() {
    Stream<byte[]> stream = Stream.of(new byte[][] {});
  }
}
