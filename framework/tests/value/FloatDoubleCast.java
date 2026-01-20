// Test case for floating-point cast value set comparison.
// Tests that the cast type must contain all values of the expression type,
// but may contain more values (containment check, not equality).

import org.checkerframework.common.value.qual.*;

public class FloatDoubleCast {

  // ============ double to float casts ============

  // Expression {1.0} -> after cast {1.0f}: Should be safe (contained in {1.0f})
  void doubleToFloatBasic() {
    @DoubleVal({1.0}) double d = 1.0;
    @DoubleVal({1.0f}) float f = (float) d;
  }

  // KEY TEST: Cast type can contain MORE values (superset) - should be safe
  // This demonstrates the fix: containment check allows supersets
  void doubleToFloatSuperset(@DoubleVal({1.0}) double d) {
    // Cast type {1.0f, 2.0f} contains all values from cast result (1.0f)
    @DoubleVal({1.0f, 2.0f}) float f = (float) d;
  }

  // ============ float to double casts ============

  // Expression {1.0f} -> after cast becomes {1.0}: Should be safe
  void floatToDoubleBasic() {
    @DoubleVal({1.0f}) float f = 1.0f;
    @DoubleVal({1.0}) double d = (double) f;
  }

  // KEY TEST: Cast type can contain MORE values (superset) - should be safe
  // This demonstrates the fix: float-to-double now uses containment check
  void floatToDoubleSuperset(@DoubleVal({1.0f}) float f) {
    // Cast type {1.0, 2.0, 3.0} contains all values of expression {1.0f}
    @DoubleVal({1.0, 2.0, 3.0}) double d = (double) f;
  }

  // ============ Special values (NaN, infinity) ============

  // NaN handling
  void specialValuesNaN() {
    @DoubleVal({Double.NaN}) double nan = Double.NaN;
    @DoubleVal({Float.NaN}) float fnan = (float) nan;
  }

  // Positive infinity
  void specialValuesPositiveInfinity() {
    @DoubleVal({Double.POSITIVE_INFINITY}) double inf = Double.POSITIVE_INFINITY;
    @DoubleVal({Float.POSITIVE_INFINITY}) float finf = (float) inf;
  }

  // Negative infinity
  void specialValuesNegativeInfinity() {
    @DoubleVal({Double.NEGATIVE_INFINITY}) double ninf = Double.NEGATIVE_INFINITY;
    @DoubleVal({Float.NEGATIVE_INFINITY}) float fninf = (float) ninf;
  }
}
