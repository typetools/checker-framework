
/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/fail/ref=DefaultConstructor.out -XDrawDiagnostics -processor checkers.nullness.NullnessChecker -Alint DefaultConstructor.java
 */
import checkers.nullness.quals.*;

public class DefaultConstructor {
  Object nullObject;
  @MonotonicNonNull Object lazyField;

  public Object getNull() { return nullObject; }
}
