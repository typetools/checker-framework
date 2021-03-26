/*
 * @test
 * @summary Test for Issue 347: concurrent semantics has desired behavior
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint Issue347.java
 * @compile/fail/ref=Issue347-con.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint Issue347.java -AconcurrentSemantics
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue347 {

  @MonotonicNonNull String mono;

  @Nullable String nble;

  void testMono() {
    if (mono == null) {
      return;
    }
    // The object referenced by mono might change, but
    // it can't become null again, even in concurrent
    // semantics.
    mono.toString();
  }

  void testNble() {
    if (nble == null) {
      return;
    }
    // error expected in concurrent semantics only
    nble.toString();
  }
}
