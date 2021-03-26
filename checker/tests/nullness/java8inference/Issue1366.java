// Test case for Issue 1366.
// https://github.com/typetools/checker-framework/issues/1366
abstract class Issue1366<T> {
  abstract <S> Issue1366<S> m1(Issue1366<S> p1, Issue1366<?> p2);

  abstract <S> Issue1366<S> m2(Issue1366<? extends S> p);

  abstract void m3(Issue1366<Number> p);

  void foo(Issue1366<Number> s) {
    s.m3(s.m2(s.m1(s, s)));
  }
}
