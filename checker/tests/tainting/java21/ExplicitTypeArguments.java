// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for record patterns whose type arguments are written explicitly. Their qualifiers are
// still inferred, because a record pattern's type cannot be annotated (JLS 14.30.1).

import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class ExplicitTypeArguments {

  record Box<T>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  static void explicit(Supplier<@Untainted String> s) {
    if (s instanceof Box<String>(var x)) {
      @Untainted String u = x;
    }
  }

  static void explicitUnbounded(Supplier<@Untainted String> s) {
    if (s instanceof Box<?>(var x)) {
      @Untainted Object u = x;
    }
  }

  static void explicitBounded(Supplier<@Untainted String> s) {
    if (s instanceof Box<? extends CharSequence>(var x)) {
      @Untainted CharSequence u = x;
    }
  }

  static void explicitBoundedTypePattern(Supplier<? extends @Untainted CharSequence> s) {
    if (s instanceof Box<? extends CharSequence>(CharSequence x)) {
      @Untainted CharSequence u = x;
    }
  }

  static void explicitTainted(Supplier<@Tainted String> s) {
    if (s instanceof Box<? extends CharSequence>(var x)) {
      // :: error: (assignment)
      @Untainted CharSequence u = x;
    }
  }

  static void noInformation(Object o) {
    if (o instanceof Box<?>(var x)) {
      // :: error: (assignment)
      @Untainted Object u = x;
    }
  }
}
