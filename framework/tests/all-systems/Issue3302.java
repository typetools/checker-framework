// Test case for Issue 3302:
// https://github.com/typetools/checker-framework/issues/3302

public class Issue3302 {
  void foo(Bar<?, ?> b) {}

  interface Bar<S, T extends Box<S> & A> {}

  interface A {}

  interface Box<U> {}
}
