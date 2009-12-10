
/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/fail/ref=DefaultConstructor.out -Anomsgtext -processor checkers.nullness.NullnessChecker -Alint DefaultConstructor.java
 */
import checkers.nullness.quals.*;

class DefaultConstructor {
  Object nullObject;
  @LazyNonNull Object lazyField;

  public Object getNull() { return nullObject; }
}
