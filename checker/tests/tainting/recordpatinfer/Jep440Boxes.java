// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// The `Box`, `test1` and `test2` declarations are copied from JEP 440: Record Patterns
// (https://openjdk.org/jeps/440), section "Type argument inference".  The annotations and the
// remaining methods are not part of JEP 440.

package recordpatinfer;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Jep440Boxes {

  // Copied from JEP 440.
  record Box<T>(T t) {}

  // Copied from JEP 440.
  static void test1(Box<Box<String>> bbs) {
    if (bbs instanceof Box<Box<String>>(Box(var s))) {
      System.out.println("String " + s);
    }
  }

  // Copied from JEP 440.
  static void test2(Box<Box<String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      System.out.println("String " + s);
    }
  }

  /**
   * One level of inference. T is {@code Box<@Untainted String>}, which is itself a parameterization
   * of Box, so the constraint <T = Box<alpha>> gives alpha = @Untainted String.
   */
  static void oneLevelUntainted(Box<@Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Untainted String ss = s;
    }
  }

  /** Same, but tainted. */
  static void oneLevelTainted(Box<@Tainted String> b) {
    if (b instanceof Box(var s)) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
  }

  /**
   * Two levels of inference: the type inferred for the outer pattern determines the type that the
   * nested pattern is matched against, which in turn is what the nested pattern's inference uses as
   * its T.
   */
  static void twoLevelsUntainted(Box<@Untainted Box<@Untainted String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      @Untainted String ss = s;
    }
  }

  /** Only the inner String is tainted, so only the inner assignment is an error. */
  static void twoLevelsInnerTainted(Box<@Untainted Box<@Tainted String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
    if (bbs instanceof Box(var inner)) {
      @Untainted Box<@Tainted String> ok = inner;
    }
  }

  /**
   * Only the outer Box's component (the inner Box itself) is tainted. The nested `Box(var s)`
   * pattern is matched against @Tainted Box<@Untainted String>, so `s` is still @Untainted, but a
   * binding for the whole inner box is @Tainted.
   */
  static void twoLevelsOuterTainted(Box<@Tainted Box<@Untainted String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      @Untainted String ss = s;
    }
    if (bbs instanceof Box(var inner)) {
      // :: error: (assignment)
      @Untainted Box<@Untainted String> bad = inner;
      @Tainted Box<@Untainted String> ok = inner;
    }
  }

  /**
   * Mixed: the outer pattern spells out its type argument, the inner one is inferred. This is JEP
   * 440's `test1`, annotated.
   */
  static void mixedExplicitAndInferred(Box<@Untainted Box<@Untainted String>> bbs) {
    // javac rejects type annotations inside a record pattern's type, so the explicit type argument
    // here has to be written unannotated; the qualifiers still have to come from inference.
    if (bbs instanceof Box<Box<String>>(Box(var s))) {
      @Untainted String ss = s;
    }
  }

  /**
   * The record component's declared type carries its own qualifier. Substituting @Untainted String
   * for T must not lose the @Untainted written on the component.
   */
  record UntaintedBox<T>(@Untainted T t) {}

  static void componentQualifier(UntaintedBox<@Tainted String> b) {
    if (b instanceof UntaintedBox(var s)) {
      // `s` should be @Untainted String: the component is declared `@Untainted T`.
      @Untainted String ss = s;
    }
  }

  /** A record with two components of different type arguments. */
  record Pair<A, B>(A a, B b) {}

  static void twoTypeArguments(Pair<@Untainted String, @Tainted String> p) {
    if (p instanceof Pair(var a, var b)) {
      @Untainted String aa = a;
      // :: error: (assignment)
      @Untainted String bb = b;
    }
  }

  /** Deeply nested, to check that inference is applied at every nesting level. */
  static void deeplyNested(Box<@Untainted Box<@Untainted Box<@Untainted String>>> bbbs) {
    if (bbbs instanceof Box(Box(Box(var s)))) {
      @Untainted String ss = s;
    }
  }

  static void deeplyNestedInnermostTainted(
      Box<@Untainted Box<@Untainted Box<@Tainted String>>> bbbs) {
    if (bbbs instanceof Box(Box(Box(var s)))) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
  }
}
