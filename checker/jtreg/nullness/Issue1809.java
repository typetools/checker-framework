/*
 * @test
 * @summary Test for Issue 1809: caching issue.
 *     https://github.com/typetools/checker-framework/issues/1809
 *     Also see framework/tests/all-systems/Issue1809.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AatfCacheSize=4 Issue1809.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AatfDoNotCache Issue1809.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Issue1809.java
 */

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
abstract class Issue1809 {

  abstract <T> Stream<T> concat(Stream<? extends T>... streams);

  abstract Optional<A> f();

  private static class A {}

  interface B {
    List<C> g();
  }

  interface C {
    List<S> h();
  }

  interface S {}

  private Stream<A> xrefsFor(B b) {
    return concat(b.g().stream().flatMap(a -> a.h().stream().map(c -> f())))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }
}
