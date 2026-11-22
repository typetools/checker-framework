// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// The type of a record pattern is inferred from the type of the matched expression.  In a loop, the
// type of a method invocation is cached during the first pass of the dataflow analysis, so a
// pattern variable can keep a qualifier that a later iteration makes wrong:
// https://github.com/typetools/checker-framework/issues/602

import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class RecordPatternInLoop {

  record Box<T>(T t) {}

  static <T> Box<T> box(T t) {
    return new Box<>(t);
  }

  static <T> Box<List<T>> boxList(T t) {
    throw new Error();
  }

  // `x` is @Tainted on the second iteration.
  void componentType(@Untainted String u, @Tainted String t, boolean b) {
    String x = u;
    while (b) {
      if (box(x) instanceof Box(var s)) {
        // TODO: false negative; see https://github.com/typetools/checker-framework/issues/602
        @Untainted String y = s;
      }
      x = t;
    }
  }

  void componentTypeArgument(@Untainted String u, @Tainted String t, boolean b) {
    String x = u;
    while (b) {
      if (boxList(x) instanceof Box(var l)) {
        // TODO: false negative; see https://github.com/typetools/checker-framework/issues/602
        @Untainted String y = l.get(0);
      }
      x = t;
    }
  }

  record ListHolder(List<String> l) {
    @SuppressWarnings("tainting") // the field's type is not @PolyTainted
    public @PolyTainted List<@PolyTainted String> l(@PolyTainted ListHolder this) {
      return l;
    }
  }

  // The type argument of the type of `l` depends on the refined qualifier of `h`, which is
  // @Tainted on the second iteration.  The type of `l` must not be cached from the first
  // iteration.
  void refinedReceiver(@Untainted ListHolder u, @Tainted ListHolder t, boolean b) {
    ListHolder h = u;
    while (b) {
      if (h instanceof ListHolder(var l)) {
        // :: error: [assignment]
        List<@Untainted String> y = l;
      }
      h = t;
    }
  }

  record Pair<A, B>(A a, B b) {}

  // Only the first type argument of the matched type differs between iterations.
  void firstTypeArgument(@Untainted String u, @Tainted String t, boolean b) {
    String x = u;
    while (b) {
      // :: error: [assignment]
      if (new Pair<>(x, u) instanceof Pair(@Untainted String first, String second)) {}
      x = t;
    }
  }

  void firstTypeArgumentVar(@Untainted String u, @Tainted String t, boolean b) {
    String x = u;
    while (b) {
      if (new Pair<>(x, u) instanceof Pair(var first, var second)) {
        // :: error: [assignment]
        @Untainted String y = first;
      }
      x = t;
    }
  }

  // The declared type of `bx` does not depend on the dataflow analysis.
  void declaredMatchedType(@Untainted String u, @Tainted String t, boolean b) {
    String x = u;
    while (b) {
      Box<List<String>> bx = boxList(x);
      if (bx instanceof Box(var l)) {
        // :: error: [assignment]
        @Untainted String y = l.get(0);
      }
      x = t;
    }
  }
}
