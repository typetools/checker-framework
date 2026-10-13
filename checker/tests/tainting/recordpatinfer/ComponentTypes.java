// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Once the type of a record pattern is inferred (JLS 26 section 18.5.5, step 6), "we can find its
// component types, which are matched against the component patterns of the record pattern".  These
// tests pin down what those component types must be, by comparing each pattern variable against the
// corresponding accessor invocation on the scrutinee: the two must always have the same annotated
// type.  Any implementation that disagrees between the two is wrong regardless of which one is
// right.
//
// The `failToMatch` example is copied from JEP 440: Record Patterns
// (https://openjdk.org/jeps/440), section "Record patterns".

package recordpatinfer;

import java.util.List;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class ComponentTypes {

  record Box<T>(T t) {}

  static void agreesWithAccessorInvariant(Box<@Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Untainted String fromPattern = s;
      @Untainted String fromAccessor = b.t();
    }
  }

  static void agreesWithAccessorTainted(Box<@Tainted String> b) {
    if (b instanceof Box(var s)) {
      // :: error: (assignment)
      @Untainted String fromPattern = s;
      // :: error: (assignment)
      @Untainted String fromAccessor = b.t();
    }
  }

  static void agreesWithAccessorExtendsWildcard(Box<? extends @Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Untainted String fromPattern = s;
      @Untainted String fromAccessor = b.t();
    }
  }

  static void agreesWithAccessorSuperWildcard(Box<? super @Untainted String> b) {
    if (b instanceof Box(var s)) {
      // :: error: (assignment)
      @Untainted Object fromPattern = s;
      // :: error: (assignment)
      @Untainted Object fromAccessor = b.t();
    }
  }

  static void agreesWithAccessorNested(Box<@Untainted Box<@Tainted String>> bbs) {
    if (bbs instanceof Box(var inner)) {
      @Untainted Box<@Tainted String> fromPattern = inner;
      @Untainted Box<@Tainted String> fromAccessor = bbs.t();
      // :: error: (assignment)
      @Untainted String innerFromAccessor = inner.t();
    }
    if (bbs instanceof Box(Box(var s))) {
      // :: error: (assignment)
      @Untainted String fromPattern = s;
    }
  }

  /** The component's declared type is a parameterized type built from the type parameter. */
  record Listy<T>(List<T> items, T first) {}

  static void parameterizedComponent(Listy<@Untainted String> l) {
    if (l instanceof Listy(var items, var first)) {
      @Untainted String fromPattern = first;
      @Untainted String fromList = items.get(0);
      @Untainted String fromAccessor = l.first();
      @Untainted String fromListAccessor = l.items().get(0);
    }
  }

  /** The component's declared type is an array of the type parameter. */
  record Arrayish<T>(T[] items) {}

  static void arrayComponent(Arrayish<@Untainted String> a) {
    if (a instanceof Arrayish(var items)) {
      @Untainted String fromPattern = items[0];
      @Untainted String fromAccessor = a.items()[0];
    }
  }

  /** A non-generic component type is unaffected by inference. */
  record Mixed<T>(T generic, @Untainted String fixed) {}

  static void nonGenericComponent(Mixed<@Tainted String> m) {
    if (m instanceof Mixed(var g, var f)) {
      @Untainted String fixedOk = f;
      // :: error: (assignment)
      @Untainted String genericBad = g;
    }
  }

  /** Primitive components: no type arguments to infer, but the pattern still has to work. */
  record Point(int x, int y) {}

  static void primitiveComponents(Object o) {
    if (o instanceof Point(int x, int y)) {
      int sum = x + y;
    }
  }

  // Copied verbatim from JEP 440.  `Pair` is not generic, so no inference happens here; this is a
  // regression guard that nested type patterns still work at all.
  static void failToMatch() {
    record Pair(Object x, Object y) {}
    Pair p = new Pair(42, 42);
    if (p instanceof Pair(String s, String t)) {
      System.out.println(s + ", " + t);
    } else {
      System.out.println("Not a pair of strings");
    }
  }

  /**
   * DESIGN QUESTION: a nested *type* pattern narrows the component type. Does `s` get the qualifier
   * of the component type of the inferred pattern type (@Untainted here), or the default for the
   * written type `String` (@Tainted, which would make the assignment an error)? This is the same
   * question as JlsExample.typePatternComponents, but with narrowing involved.
   */
  static void narrowingNestedTypePattern(Box<@Untainted Object> b) {
    if (b instanceof Box(String s)) {
      @Untainted String ss = s;
    }
  }
}
