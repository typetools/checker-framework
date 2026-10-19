// Test case for https://github.com/typetools/checker-framework/issues/1256

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class Issue1256 {

  @MonotonicNonNull Object defaultFoo;

  public void test(Object foo, boolean condition) {
    Object bar = condition ? foo : defaultFoo;
  }
}
