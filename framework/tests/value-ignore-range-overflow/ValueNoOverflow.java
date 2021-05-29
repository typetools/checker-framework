import org.checkerframework.common.value.qual.*;

public class ValueNoOverflow {

  // This file contains a series of simple smoke tests for IntRange and ArrayLenRange with no
  // overflow.

  void test_plus(@IntRange(from = 0) int x, @IntRange(from = -1) int z) {
    @IntRange(from = 1) int y = x + 1; // IntRange(from = 0) to IntRange(from = 1)
    @IntRange(from = 0) int w = z + 1; // GTEN1 to NN
  }

  void test_minus(@IntRange(to = 0) int x, @IntRange(to = 1) int z) {
    @IntRange(to = -1) int y = x - 1; // IntRange(from = 0) to GTEN1
    @IntRange(to = 0) int w = z - 1; // Pos to NN
  }

  void test_mult(@IntRange(from = 0) int x, @IntRange(from = 1) int z) {
    @IntRange(from = 0) int y = x * z;
    @IntRange(from = 1) int w = z * z;
  }

  void testLong_plus(@IntRange(from = 0) long x, @IntRange(from = -1) long z) {
    @IntRange(from = 1) long y = x + 1; // IntRange(from = 0) to IntRange(from = 1)
    @IntRange(from = 0) long w = z + 1; // GTEN1 to NN
  }

  void testLong_minus(@IntRange(to = 0) long x, @IntRange(to = 1) long z) {
    @IntRange(to = -1) long y = x - 1; // IntRange(from = 0) to GTEN1
    @IntRange(to = 0) long w = z - 1; // Pos to NN
  }

  void testLong_mult(@IntRange(from = 0) long x, @IntRange(from = 1) long z) {
    @IntRange(from = 0) long y = x * z;
    @IntRange(from = 1) long w = z * z;
  }

  void testShort_plus(@IntRange(from = 0) short x, @IntRange(from = -1) short z) {
    @IntRange(from = 1) short y = (short) (x + 1); // IntRange(from = 0) to IntRange(from = 1)
    @IntRange(from = 0) short w = (short) (z + 1); // GTEN1 to NN
  }

  void testShort_minus(@IntRange(to = 0) short x, @IntRange(to = 1) short z) {
    @IntRange(to = -1) short y = (short) (x - 1); // IntRange(from = 0) to GTEN1
    @IntRange(to = 0) short w = (short) (z - 1); // Pos to NN
  }

  void testShort_mult(@IntRange(from = 0) short x, @IntRange(from = 1) short z) {
    @IntRange(from = 0) short y = (short) (x * z);
    @IntRange(from = 1) short w = (short) (z * z);
  }

  void testChar_plus(@IntRange(from = 0) char x) {
    @IntRange(from = 1) char y = (char) (x + 1); // IntRange(from = 0) to IntRange(from = 1)
  }

  void testChar_minus(@IntRange(to = 65534) char z) {
    @IntRange(to = 65533) char w = (char) (z - 1); // IntRange(to = 65535) to IntRange(to = 65535)
  }

  void testChar_mult(@IntRange(from = 0) char x, @IntRange(from = 1) char z) {
    @IntRange(from = 0) char y = (char) (x * z);
    @IntRange(from = 1) char w = (char) (z * z);
  }

  void testByte_plus(@IntRange(from = 0) byte x, @IntRange(from = -1) byte z) {
    @IntRange(from = 1) byte y = (byte) (x + 1); // IntRange(from = 0) to IntRange(from = 1)
    @IntRange(from = 0) byte w = (byte) (z + 1); // GTEN1 to NN
  }

  void testByte_minus(@IntRange(to = 0) byte x, @IntRange(to = 1) byte z) {
    @IntRange(to = -1) byte y = (byte) (x - 1); // IntRange(from = 0) to GTEN1
    @IntRange(to = 0) byte w = (byte) (z - 1); // Pos to NN
  }

  void testByte_mult(@IntRange(from = 0) byte x, @IntRange(from = 1) byte z) {
    @IntRange(from = 0) byte y = (byte) (x * z);
    @IntRange(from = 1) byte w = (byte) (z * z);
  }

  void test_casting(@IntRange(from = 0) int i) {
    @IntRange(from = 1, to = ((long) Integer.MAX_VALUE) + 1)
    long x = i + 1;
  }

  // Include ArrayLenRange tests once ArrayLenRange is merged.

  void arraylenrange_test(int @ArrayLenRange(from = 5) [] a) {
    int @ArrayLenRange(from = 7) [] b = new int[a.length + 2];
  }

  void arraylenrange_test2(int @ArrayLenRange(to = 5) [] a) {
    int @ArrayLenRange(from = 0, to = 0) [] b = new int[a.length - 5];
  }
}
