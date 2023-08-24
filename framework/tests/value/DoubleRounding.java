// Test case for https://github.com/typetools/checker-framework/issues/6141 .
// @skip-test until the bug is fixed

public class DoubleRounding {

  final double FLOATING_POINT_DELTA = 1e-15;

  void round() {
    float f = (float) FLOATING_POINT_DELTA;
  }
}
