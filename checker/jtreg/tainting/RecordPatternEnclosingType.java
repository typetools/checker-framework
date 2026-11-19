/*
 * @test
 * @summary Test that the type of a `var` pattern variable is the upward projection of the record
 * component type, including the enclosing type of an inner class type.
 *
 * This test compares the full text of the diagnostic, because the "// :: error:" test harness
 * matches only the message key. The type checker's subtyping does not compare the type arguments
 * of an enclosing type, so only the text of a diagnostic shows a captured type variable there.
 *
 * @requires jdk.version.major >= 21
 * @compile/fail/ref=RecordPatternEnclosingType.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker RecordPatternEnclosingType.java
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class RecordPatternEnclosingType {
  static class Outer<T> {
    class In {}
  }

  record Q<T>(Outer<T>.In in) {}

  void test(Q<? extends @Untainted String> q) {
    if (q instanceof Q(var in)) {
      @Untainted Object o = in;
    }
  }
}
