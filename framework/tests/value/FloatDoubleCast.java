// Test case for floating-point cast value set comparison.
// Tests that the cast type must contain all values of the expression type,
// but may contain more values (containment check, not equality).

import org.checkerframework.common.value.qual.*;

public class FloatDoubleCast {

  // ============ float to float casts ============

  void floatToFloatBasic() {
    @DoubleVal({1.0}) float f = 1.0f;
    @DoubleVal({1.0}) float f2 = (float) f;
  }

  void floatToFloatSuperset(@DoubleVal({1.0}) float f) {
    @DoubleVal({1.0, 2.0, 3.0}) float f2 = (float) f;
  }

  void floatToFloatSubset(@DoubleVal({1.0, 2.0, 3.0}) float f) {
    // :: error: (assignment)
    @DoubleVal({1.0}) float f2 = (float) f;
  }

  void floatToFloatPrecision() {
    @DoubleVal({3.1415927f}) float f1 = 3.1415927f;
    @DoubleVal({3.1415927f}) float f2 = (float) f1;
  }

  void floatToFloatUnannotated1(@DoubleVal({1.0}) float f1) {
    float f2 = f1;
  }

  void floatToFloatUnannotated2(float f1) {
    // :: error: (assignment)
    @DoubleVal({1.0}) float f2 = f1;
  }

  // ============ double to float casts ============

  void doubleToFloatBasic() {
    @DoubleVal({1.0}) double d = 1.0;
    @DoubleVal({1.0}) float f = (float) d;
  }

  void doubleToFloatSuperset(@DoubleVal({1.0}) double d) {
    @DoubleVal({1.0, 2.0}) float f = (float) d;
  }

  void doubleToFloatSubset(@DoubleVal({1.0, 2.0}) double d) {
    // :: error: (assignment)
    @DoubleVal({1.0}) float f = (float) d;
  }

  void doubleToFloatOutOfRange() {
    @DoubleVal({1e40}) double d = 1e40;
    @DoubleVal({Float.POSITIVE_INFINITY}) float f = (float) d;
  }

  void doubleToFloatReducedPrecision1() {
    @DoubleVal({4.9E-324}) double d = Double.MIN_VALUE;
    @DoubleVal({0}) float f = (float) d;
  }

  void doubleToFloatReducedPrecision2() {
    @DoubleVal({3.141592653589793}) double d = Math.PI;
    @DoubleVal({3.1415927410125732}) float f = (float) d;
    @DoubleVal({3.1415927f}) float f2 = (float) d;
  }

  // ============ float to double casts ============

  void floatToDoubleBasic() {
    @DoubleVal({1.0}) float f = 1.0f;
    @DoubleVal({1.0}) double d = (double) f;
  }

  void floatToDoubleSuperset(@DoubleVal({1.0}) float f) {
    @DoubleVal({1.0, 2.0, 3.0}) double d = (double) f;
  }

  void floatToDoubleSubset(@DoubleVal({1.0, 2.0, 3.0}) float f) {
    // :: error: (assignment)
    @DoubleVal({1.0}) double d = (double) f;
  }

  // ============ double to double casts ============

  void doubleToDoubleBasic() {
    @DoubleVal({1.0}) double d1 = 1.0;
    @DoubleVal({1.0}) double d = (double) d1;
  }

  void doubleToDoubleSuperset(@DoubleVal({1.0}) double d1) {
    @DoubleVal({1.0, 2.0, 3.0}) double d = (double) d1;
  }

  void doubleToDoubleSubset(@DoubleVal({1.0, 2.0, 3.0}) double d1) {
    // :: error: (assignment)
    @DoubleVal({1.0}) double d = (double) d1;
  }

  // ============ Special values (NaN, infinity) ============

  void specialValuesNaN() {
    @DoubleVal({Double.NaN}) double nan = Double.NaN;
    @DoubleVal({Float.NaN}) float fnan = (float) nan;
  }

  void specialValuesPositiveInfinity() {
    @DoubleVal({Double.POSITIVE_INFINITY}) double inf = Double.POSITIVE_INFINITY;
    @DoubleVal({Float.POSITIVE_INFINITY}) float finf = (float) inf;
  }

  void specialValuesNegativeInfinity() {
    @DoubleVal({Double.NEGATIVE_INFINITY}) double ninf = Double.NEGATIVE_INFINITY;
    @DoubleVal({Float.NEGATIVE_INFINITY}) float fninf = (float) ninf;
  }
}
