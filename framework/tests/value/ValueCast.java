// Test case for issue 1299: https://github.com/typetools/checker-framework/issues/1299

import org.checkerframework.common.value.qual.*;

public class ValueCast {
  void testShort_plus(@IntRange(from = 0) short x) {
    @IntRange(from = 1, to = Short.MAX_VALUE + 1) int y = x + 1;
    // :: error: (assignment)
    @IntRange(from = 1, to = Short.MAX_VALUE - 1) int z = x;
  }

  void testIntFrom(@IntRange(from = 0) int x) {
    @IntRange(from = 0, to = Integer.MAX_VALUE) long y = x;
    // :: error: (assignment)
    @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int z = x;
  }

  void testShortFrom(@IntRange(from = 0) short x) {
    @IntRange(from = 0, to = Short.MAX_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(from = 0, to = Short.MAX_VALUE - 1) int z = x;
  }

  void testCharFrom(@IntRange(from = 0) char x) {
    @IntRange(from = 0, to = Character.MAX_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(from = 0, to = Character.MAX_VALUE - 1) int z = x;
  }

  void testByteFrom(@IntRange(from = 0) byte x) {
    @IntRange(from = 0, to = Byte.MAX_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(from = 0, to = Byte.MAX_VALUE - 1) int z = x;
  }

  void testIntTo(@IntRange(to = 0) int x) {
    @IntRange(to = 0, from = Integer.MIN_VALUE) long y = x;
    // :: error: (assignment)
    @IntRange(to = 0, from = Integer.MIN_VALUE + 1) int z = x;
  }

  void testShortTo(@IntRange(to = 0) short x) {
    @IntRange(to = 0, from = Short.MIN_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(to = 0, from = Short.MIN_VALUE + 1) int z = x;
  }

  void testCharTo(@IntRange(to = 1) char x) {
    @IntRange(to = 1, from = Character.MIN_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(to = 1, from = Character.MIN_VALUE + 1) int z = x;
  }

  void testByteTo(@IntRange(to = 0) byte x) {
    @IntRange(to = 0, from = Byte.MIN_VALUE) int y = x;
    // :: error: (assignment)
    @IntRange(to = 0, from = Byte.MIN_VALUE + 1) int z = x;
  }
}
