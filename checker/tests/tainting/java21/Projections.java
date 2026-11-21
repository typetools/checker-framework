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
import java.util.function.Consumer;
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
   * resolution did not introduce, so upward projection does not remove CAP#1 from the type of the
   * pattern.
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

  /**
   * The upward projection of the array type {@code CAP#1[]} is an array of the upward projection of
   * CAP#1.
   */
  static void arrayUpward(ArrayBox<? extends @Untainted String> a) {
    if (a instanceof ArrayBox(var items)) {
      @Untainted String s = items[0];
    }
  }

  static void arrayUpwardTainted(ArrayBox<? extends @Tainted String> a) {
    if (a instanceof ArrayBox(var items)) {
      // :: error: [assignment]
      @Untainted String s = items[0];
    }
  }

  record SuperArray<T>(List<? super T[]> sink) {}

  /**
   * Upward projection of the component type replaces {@code CAP#1[]}, the lower bound of the
   * wildcard in {@code List<? super CAP#1[]>}, with the downward projection of {@code CAP#1[]},
   * which is {@code @Untainted String[]}, the array of the lower bound of CAP#1.
   */
  static void arrayDownward(SuperArray<? super @Untainted String> s) {
    if (s instanceof SuperArray(var sink)) {
      List<? super @Untainted String[]> ok = sink;
      // :: error: [assignment]
      List<? super @Tainted String[]> bad = sink;
    }
  }

  /**
   * CAP#1 has no lower bound, so the downward projection of {@code CAP#1[]} is undefined, and the
   * wildcard {@code ? super CAP#1[]} becomes {@code ?}.
   */
  static void arrayDownwardUndefined(SuperArray<? extends @Untainted String> s) {
    if (s instanceof SuperArray(var sink)) {
      List<?> ok = sink;
      // :: error: [assignment]
      List<? extends @Untainted Object> bad = sink;
    }
  }

  record SuperList<T>(List<? super List<? extends T>> sink) {}

  /**
   * Upward projection of the component type replaces {@code List<? extends CAP#1>}, the lower bound
   * of the wildcard in {@code List<? super List<? extends CAP#1>>}, with the downward projection of
   * {@code List<? extends CAP#1>}, which is the parameterized type {@code List<? extends @Untainted
   * String>}.
   */
  static void declaredDownwardInLowerBound(SuperList<? super @Untainted String> s) {
    if (s instanceof SuperList(var sink)) {
      List<? super List<? extends @Untainted String>> ok = sink;
      // :: error: [assignment]
      List<? super List<? extends @Tainted String>> bad = sink;
    }
  }

  /**
   * CAP#1 has no lower bound, so the downward projection of {@code List<? extends CAP#1>} is
   * undefined, and the wildcard {@code ? super List<? extends CAP#1>} becomes {@code ?}.
   */
  static void wildcardDownwardUndefined(SuperList<? extends @Untainted String> s) {
    if (s instanceof SuperList(var sink)) {
      List<?> ok = sink;
      // :: error: [assignment]
      List<? extends @Untainted Object> bad = sink;
    }
  }

  record ExactSuperList<T>(List<? super List<T>> sink) {}

  /**
   * The downward projection of {@code List<CAP#1>} is undefined, because its type argument mentions
   * CAP#1 and is not a wildcard, so the wildcard {@code ? super List<CAP#1>} becomes {@code ?}.
   */
  static void typeArgumentDownwardUndefined(ExactSuperList<? super @Untainted String> s) {
    if (s instanceof ExactSuperList(var sink)) {
      List<?> ok = sink;
      // :: error: [assignment]
      List<? extends @Untainted Object> bad = sink;
    }
  }

  record SuperSuperList<T>(List<? super List<? super T>> sink) {}

  /**
   * The downward projection of {@code List<? super CAP#1>} is {@code List<? super U>}, where U is
   * the upward projection of CAP#1.
   */
  static void superWildcardDownward(SuperSuperList<? extends @Untainted String> s) {
    if (s instanceof SuperSuperList(var sink)) {
      List<? super List<? super @Untainted String>> ok = sink;
    }
  }

  static void superWildcardDownwardTainted(SuperSuperList<? extends @Tainted String> s) {
    if (s instanceof SuperSuperList(var sink)) {
      // :: error: [assignment]
      List<? super List<? super @Untainted String>> bad = sink;
    }
  }

  record AnnotatedSuperList<T>(List<? super @Untainted T> sink) {}

  /**
   * The downward projection of {@code @Untainted CAP#1} is the lower bound of CAP#1 with the
   * primary qualifier of {@code @Untainted CAP#1}, not that of the lower bound.
   */
  static void annotatedTypeVariableDownward(AnnotatedSuperList<? super @Tainted String> s) {
    if (s instanceof AnnotatedSuperList(var sink)) {
      List<? super @Untainted String> ok = sink;
      // :: error: [assignment]
      List<? super @Tainted String> bad = sink;
    }
  }

  static class Outer<T> {
    class In {}
  }

  record SuperInner<T>(List<? super Outer<T>.In> sink) {}

  /**
   * The downward projection of the enclosing type {@code Outer<CAP#1>} is undefined, so the
   * wildcard {@code ? super Outer<CAP#1>.In} becomes {@code ?}.
   */
  static void enclosingTypeDownwardUndefined(SuperInner<? extends @Untainted String> s) {
    if (s instanceof SuperInner(var sink)) {
      List<?> ok = sink;
      // :: error: [assignment]
      List<? extends @Untainted Object> bad = sink;
    }
  }

  record ConsumerBox<T>(Consumer<? super T> consumer) {}

  /**
   * CAP#1 has no lower bound, so the upward projection of {@code Consumer<? super CAP#1>} is {@code
   * Consumer<?>}, whose wildcard has the upper bound of {@code ? super CAP#1}.
   */
  static void superWildcardUpward(ConsumerBox<? extends @Untainted String> b) {
    if (b instanceof ConsumerBox(var consumer)) {
      Consumer<?> ok = consumer;
      // :: error: [assignment]
      Consumer<? extends @Untainted Object> bad = consumer;
    }
  }

  static class ListBoxHolder<A, B extends Box<List<A>>> {
    B box() {
      throw new Error();
    }
  }

  /**
   * The matched type is the captured type variable CAP#2, whose upper bound {@code
   * Box<List<CAP#1>>} mentions CAP#1. The type of the pattern is {@code Box<List<CAP#1>>}: only the
   * fresh type variables that resolution introduces are projected away, and there are none.
   */
  static void capturedInBoundOfMatchedType(ListBoxHolder<? extends @Untainted String, ?> h) {
    if (h.box() instanceof Box(var l)) {
      @Untainted String s = l.get(0);
    }
    if (h.box() instanceof Box(List<? extends @Untainted String> l)) {
      @Untainted String s = l.get(0);
    }
  }

  static void capturedInBoundOfMatchedTypeTainted(ListBoxHolder<? extends @Tainted String, ?> h) {
    if (h.box() instanceof Box(var l)) {
      // :: error: [assignment]
      @Untainted String s = l.get(0);
    }
  }

  static class ListHolder<E extends List<?>> {}

  record HolderBox<T>(ListHolder<List<? extends T>> holder) {}

  /**
   * Upward projection of the component type replaces the type argument {@code List<? extends
   * CAP#1>} with {@code ? super L}, where L is the downward projection of the type argument,
   * because the declared bound of E, {@code List<?>}, is a subtype of the upward projection of the
   * type argument, {@code List<?>} (JLS 4.10.5).
   */
  static void declaredDownwardInTypeArgument(HolderBox<? super @Untainted String> h) {
    if (h instanceof HolderBox(var holder)) {
      ListHolder<? super List<? extends @Untainted String>> ok = holder;
      // :: error: [assignment]
      ListHolder<? super List<? extends @Tainted String>> bad = holder;
    }
  }

  record IntersectionHolderBox<T extends Number & Runnable>(ListHolder<List<? super T>> holder) {}

  /**
   * The downward projection of the type argument {@code List<? super CAP#1>} would be {@code List<?
   * super Number & Runnable>}, but a wildcard bound cannot be an intersection type, so the downward
   * projection is undefined, and the type argument becomes {@code ?}.
   */
  static void intersectionDownwardUndefined(IntersectionHolderBox<?> h) {
    if (h instanceof IntersectionHolderBox(var holder)) {
      ListHolder<?> ok = holder;
      // :: error: [assignment]
      ListHolder<? super List<? super Number>> bad = holder;
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
      // :: error: [assignment]
      @Untainted Number n = t;
    }
  }
}
