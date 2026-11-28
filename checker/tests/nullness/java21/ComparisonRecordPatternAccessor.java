// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// Matching a record pattern calls the record's component accessors, which might write fields.
public class ComparisonRecordPatternAccessor {

  static @Nullable String g;

  record Impure(String x) {
    public String x() {
      g = null;
      return x;
    }
  }

  record Pure1(String x) {
    @Pure
    public String x() {
      return x;
    }
  }

  record Outer(Impure inner) {
    @Pure
    public Impure inner() {
      return inner;
    }
  }

  void impureAccessorInSwitch(Impure r, String expected) {
    if (g
        == (switch (r) {
          case Impure(var x) -> expected;
        })) {
      // The accessor `x()` is called after `g` is read, and it sets `g` to null.
      // :: error: [dereference.of.nullable]
      g.toString();
    }
  }

  void impureAccessorInInstanceof(Object o) {
    if (g == (o instanceof Impure(var x) ? "a" : "b")) {
      // The accessor `x()` is called after `g` is read, and it sets `g` to null.
      // :: error: [dereference.of.nullable]
      g.toString();
    }
  }

  void impureAccessorInNestedPattern(Outer o, String expected) {
    if (g
        == (switch (o) {
          case Outer(Impure(var x)) -> expected;
        })) {
      // The accessor `x()` is called after `g` is read, and it sets `g` to null.
      // :: error: [dereference.of.nullable]
      g.toString();
    }
  }

  void pureAccessor(Pure1 r, String expected) {
    if (g
        == (switch (r) {
          case Pure1(var x) -> expected;
        })) {
      g.toString();
    }
  }
}
