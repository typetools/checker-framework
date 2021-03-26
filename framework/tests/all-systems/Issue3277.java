// Test case for Issue 3277:
// https://github.com/typetools/checker-framework/issues/3277

// Any arbitrary annotation can be used.
import org.checkerframework.common.aliasing.qual.MaybeAliased;

public class Issue3277 {
  <T> void f() {
    Object o = new @MaybeAliased Generic<?>[0];
    o = new Generic<@MaybeAliased ?>[0];
  }

  // TODO: Having the same code in fields crashes javac
  // with an AssertionError.
  // Object o1 = new @MaybeAliased Generic<?>[0];
  // Object o2 = new Generic<@MaybeAliased ?>[0];

  class Generic<U> {}
}
