import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * Test case for scope cleanup of dependent type annotations.
 *
 * <p>After a local variable goes out of scope, a dependent type annotation that references that
 * variable must not be interpreted as referring to a different variable with the same name. This
 * test verifies that an annotation like @LessThan("b") on variable 'a' is dropped when a different
 * variable named 'b' comes into scope.
 */
public class ScopeCleanup {

  /**
   * Test that a @LessThan annotation does not refer to a different variable with the same name as
   * the variable that went out of scope.
   */
  void testScopeCleanup() {
    int a = 5;

    {
      int b = 10;
      a = b - 1;
      // At this point, 'a' should have type @LessThan("b") and that's correct.

      // This should be fine: a < b is true
      @LessThan("b") int shouldWork = a;
    }

    // After exiting scope, 'b' is no longer in scope.
    // Non-dependent annotations on 'a' survive.
    @NonNegative int ok = a; // This should still work - a is still non-negative (a=9)

    {
      int b = 3; // Different variable 'b' with a smaller value!
      // The old @LessThan("b") annotation on 'a' must not be interpreted as referring to this 'b'.
      // a = 9 and this b = 3, so a > b, not a < b.
      // :: error: (assignment)
      @LessThan("b") int shouldFail = a;
    }
  }

  /**
   * Test that method parameters are not affected by scope cleanup (they should always be considered
   * in scope).
   */
  void testParametersRemainInScope(@NonNegative int param) {
    int x;
    {
      int localVar = param + 1;
      x = param - 1; // x < param should be valid
    }
    // Even after the block ends, 'param' is still in scope, so @LessThan("param") should be valid
    // This test ensures method parameters are handled correctly
    @LessThan("param") int shouldWork = x;
  }
}
