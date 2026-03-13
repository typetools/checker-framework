import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * Test case for scope cleanup of dependent type annotations.
 *
 * <p>When a local variable goes out of scope, any dependent type annotation that references that
 * variable should be invalidated. This test verifies that annotations like @LessThan("b") on
 * variable 'a' are properly dropped when 'b' goes out of scope.
 */
public class ScopeCleanup {

  /**
   * Test that @LessThan annotation is properly cleaned up when the referenced variable goes out of
   * scope.
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
    // The @LessThan("b") annotation on 'a' should be dropped.
    // However, non-dependent annotations should survive.
    @NonNegative int ok = a; // This should still work - a is still non-negative (a=9)

    {
      int b = 3; // Different variable 'b' with a smaller value!
      // Without the fix, the old @LessThan("b") annotation would incorrectly refer to THIS 'b'.
      // But a = 9 and this b = 3, so a > b, not a < b.

      // This should be an error - we can no longer prove a < b
      // The assignment below should fail type-checking because we can't prove a < b
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
