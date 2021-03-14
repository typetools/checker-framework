import org.checkerframework.checker.index.qual.LessThan;

class LessThanFloatLiteral {
  void test(int x) {
    if (1.0 > x) {
      // TODO: It might be nice to handle comparisons against floats,
      // but an array index is not generally compared to a float.
      // :: error: assignment.type.incompatible
      @LessThan("1") int y = x;
    }
  }
}
