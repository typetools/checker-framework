// Some of these code was submitted in #1384.
// https://github.com/typetools/checker-framework/issues/1384
// Other parts are from the following comment.
// https://github.com/typetools/checker-framework/pull/1387#issuecomment-316147360
// The rest is from plume-lib.

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class FalsePositives {
  static class Partitioning<F> {}

  public static <T> List<Partitioning<T>> partitionInto(Queue<T> elts, int k) {
    if (elts.size() < k) {
      throw new IllegalArgumentException();
    }
    return partitionIntoHelper(elts, Arrays.asList(new Partitioning<T>()), k, 0);
  }

  public static <T> List<Partitioning<T>> partitionIntoHelper(
      Queue<T> elts, List<Partitioning<T>> resultSoFar, int numEmptyParts, int numNonemptyParts) {
    throw new RuntimeException();
  }

  interface Box<T> {}

  interface Function<P, R> {
    R apply(P p);
  }

  interface Utils {
    <I, O> Box<O> foo(Box<I> input, Function<? super I, ? extends O> function);

    <I, O> Function<I, O> bar(Function<? super I, ? extends O> function);
  }

  class Test {
    Box<Integer> demo(Utils u, Box<String> bs) {
      return u.foo(bs, u.bar((String s) -> 5));
    }

    Integer ugh(String n) {
      return 5;
    }

    Box<Integer> demo2(Utils u, Box<String> bs) {
      return u.foo(bs, u.bar(this::ugh));
    }
  }

  abstract class Test2 {
    abstract <T> Box<Box<T>> foo(Box<? extends Box<? extends T>> p);

    abstract <T> Box<Box<T>> bar(Function<Number, T> f);

    abstract String baz(Number p);

    Box<Box<String>> demo() {
      return foo(bar(this::baz));
    }
  }
}
