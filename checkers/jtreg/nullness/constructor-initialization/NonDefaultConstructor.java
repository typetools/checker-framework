
/*
 * @test
 * @summary Test that the stub files get invoked
 * @compile/fail/ref=DefaultConstructor.out -Anomsgtext -processor checkers.nullness.NullnessChecker -Alint DefaultConstructor.java
 */
import checkers.nullness.quals.*;

class NonDefaultConstructor {
  Object nullObject;
  @LazyNonNull Object lazyField;

  // error doesn't initialize nullObject
  public NonDefaultConstructor() { }

  // error doesn't initialize nullObject
  public NonDefaultConstructor(int i) {
      lazyField = "m";
  }

  // OK, lazyField is lazy!
  public NonDefaultConstructor(double a) {
      nullObject = "n";
  }

  public NonDefaultConstructor(String s) {
      nullObject = "a";
      lazyField = "m";
  }

  public Object getNull() { return nullObject; }
}
