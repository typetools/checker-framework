// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for step 6 of JLS 26 section 18.5.5, which takes the upward projection of the record
// pattern's type with respect to the fresh type variables that resolution introduced (JLS 4.10.5).

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Projections {

  record Box<T>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  record NumBox<T extends Number>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  /**
   * The fresh type variable for T has the upper bound {@code Number & @Untainted Serializable}, an
   * intersection type, whose projection is the intersection of the projections of its elements.
   */
  static void intersection(Supplier<? extends @Untainted Serializable> s) {
    if (s instanceof NumBox(var x)) {
      @Untainted Serializable ser = x;
      @Untainted Number n = x;
    }
  }

  static <T> Supplier<List<T>> wrap(List<T> list) {
    throw new Error();
  }

  /**
   * The matched type, {@code Supplier<List<CAP#1>>}, mentions a captured type variable that
   * resolution did not introduce, so it is not projected away from the type of the pattern.
   */
  static void capturedInMatchedType(List<? extends @Untainted String> list) {
    if (wrap(list) instanceof Box(var l)) {
      @Untainted String s = l.get(0);
    }
    if (wrap(list) instanceof Box(List<? extends @Untainted String> l)) {
      @Untainted String s = l.get(0);
    }
  }
}
