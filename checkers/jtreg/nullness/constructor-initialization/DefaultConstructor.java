
/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/ref=DefaultConstructor.out -processor checkers.nullness.NullnessChecker -Alint DefaultConstructor.java
 */
import checkers.nullness.quals.*;

public class DefaultConstructor {
  Object nullObject;
  @MonotonicNonNull Object lazyField;

  public Object getNull() { return nullObject; }
}
