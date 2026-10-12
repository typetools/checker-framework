// Test case for https://github.com/typetools/checker-framework/issues/8323 .
// framework/tests/all-systems/Issue8323.java checks that there is no crash; this test checks the
// qualifiers that inference keeps.

import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue8323Tainting {
  interface FB<K, V extends Comparable<V>> {}

  interface Box<T> {
    <R> Box<R> map(Function<? super T, ? extends R> f);

    <S> S get(Getter<? super T, S> g);
  }

  interface Getter<T, S> {}

  static native <T> Getter<T, List<T>> toList();

  static native <P> FB<P, ? extends Integer> fbExtends(P p);

  static native <P> FB<P, ?> fbUnbounded(P p);

  static List<FB<@Untainted String, ?>> untainted(Box<@Untainted String> b) {
    return b.map(t -> fbExtends(t)).get(toList());
  }

  static List<FB<@Untainted String, ?>> tainted(Box<String> b) {
    // :: error: (type.arguments.not.inferred)
    return b.map(t -> fbExtends(t)).get(toList());
  }

  static List<FB<@Untainted String, ?>> untaintedUnbounded(Box<@Untainted String> b) {
    return b.map(t -> fbUnbounded(t)).get(toList());
  }

  static List<FB<@Untainted String, ?>> taintedUnbounded(Box<String> b) {
    // :: error: (type.arguments.not.inferred)
    return b.map(t -> fbUnbounded(t)).get(toList());
  }

  interface Self<V> {}

  static class TaintedSelf implements Self<@Tainted TaintedSelf> {}

  interface SelfBounded<K, V extends Self<V>> {}

  // The capture of `? extends @Untainted TaintedSelf` must be @Untainted, but its bound
  // Self<@Tainted TaintedSelf> requires it to be @Tainted.
  // :: error: (type.argument)
  static native <P> SelfBounded<P, ? extends @Untainted TaintedSelf> selfBounded(P p);

  static List<SelfBounded<String, ?>> conflictingCapture(Box<String> b) {
    // :: error: (type.arguments.not.inferred)
    return b.map(t -> selfBounded(t)).get(toList());
  }
}
