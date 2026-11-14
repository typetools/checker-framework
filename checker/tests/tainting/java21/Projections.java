// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for upward and downward projection (JLS 4.10.5).  Step 6 of JLS 26 section 18.5.5 takes
// the upward projection of the record pattern's type with respect to the fresh type variables that
// resolution introduced, and a `var` binding variable has the upward projection of the type of its
// record component.

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Tainted;
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

  record ArrayBox<T>(T[] items) {}

  /** The upward projection of the array type {@code CAP#1[]} is an array of the projection. */
  static void arrayUpward(ArrayBox<? extends @Untainted String> a) {
    if (a instanceof ArrayBox(var items)) {
      @Untainted String s = items[0];
    }
  }

  static void arrayUpwardTainted(ArrayBox<? extends @Tainted String> a) {
    if (a instanceof ArrayBox(var items)) {
      // :: error: (assignment)
      @Untainted String s = items[0];
    }
  }

  record SuperArray<T>(List<? super T[]> sink) {}

  /**
   * The lower bound of the wildcard in {@code List<? super CAP#1[]>} is replaced by its downward
   * projection, {@code @Untainted String[]}, the array of the lower bound of CAP#1.
   */
  static void arrayDownward(SuperArray<? super @Untainted String> s) {
    if (s instanceof SuperArray(var sink)) {
      List<? super @Untainted String[]> ok = sink;
      // :: error: (assignment)
      List<? super @Tainted String[]> bad = sink;
    }
  }

  record SuperList<T>(List<? super List<? extends T>> sink) {}

  /**
   * The lower bound of the wildcard in {@code List<? super List<? extends CAP#1>>} is replaced by
   * its downward projection, the parameterized type {@code List<? extends @Untainted String>}.
   */
  static void declaredDownwardInLowerBound(SuperList<? super @Untainted String> s) {
    if (s instanceof SuperList(var sink)) {
      List<? super List<? extends @Untainted String>> ok = sink;
      // :: error: (assignment)
      List<? super List<? extends @Tainted String>> bad = sink;
    }
  }

  static class ListHolder<E extends List<?>> {}

  record HolderBox<T>(ListHolder<List<? extends T>> holder) {}

  /**
   * The type argument {@code List<? extends CAP#1>} is replaced by {@code ? super L}, where L is
   * its downward projection, because the declared bound of E, {@code List<?>}, is a subtype of its
   * upward projection, {@code List<?>} (JLS 4.10.5).
   */
  static void declaredDownwardInTypeArgument(HolderBox<? super @Untainted String> h) {
    if (h instanceof HolderBox(var holder)) {
      ListHolder<? super List<? extends @Untainted String>> ok = holder;
      // :: error: (assignment)
      ListHolder<? super List<? extends @Tainted String>> bad = holder;
    }
  }

  record Bounded<T extends @Untainted Number & Comparable<T>>(T t) {}

  /**
   * The upper bound of CAP#1 is the intersection type {@code @Untainted Number &
   * Comparable<CAP#1>}, which mentions CAP#1. Its upward projection, the type of {@code t}, is
   * {@code @Untainted Number & Comparable<? extends @Untainted Number>}. A wildcard bound cannot be
   * an intersection type, so the type of the record pattern is {@code Bounded<?>}.
   */
  static void intersectionUpward(Bounded<?> b) {
    if (b instanceof Bounded(var t)) {
      @Untainted Number n = t;
      Comparable<?> c = t;
    }
  }

  record TaintedBounded<T extends Number & Comparable<T>>(T t) {}

  static void intersectionUpwardTainted(TaintedBounded<?> b) {
    if (b instanceof TaintedBounded(var t)) {
      // :: error: (assignment)
      @Untainted Number n = t;
    }
  }
}
