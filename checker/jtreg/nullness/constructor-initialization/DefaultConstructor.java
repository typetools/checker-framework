/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/fail/ref=DefaultConstructor.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint DefaultConstructor.java
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class DefaultConstructor {
  Object nullObject;
  @MonotonicNonNull Object lazyField;

  public Object getNull() {
    return nullObject;
  }
}
