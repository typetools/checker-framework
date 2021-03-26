// Test case for issue 3667:
// https://github.com/typetools/checker-framework/issues/3667

import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class UnsignedRightShiftTest {
  int length;

  void unsignedRightShiftWithLiteral() {
    int length = Integer.MAX_VALUE;
    byte b = (byte) (length >>> 24);
  }

  void unsignedRightShiftWithParameter(int length) {
    byte b1 = (byte) (length >>> 24);
    byte b2 = (@Signed byte) (length >>> 24);
    byte b3 = (@Unsigned byte) (length >>> 24);
  }

  void unsignedRightShiftWithField() {
    byte b = (byte) (this.length >>> 24);
  }

  void unsignedRightShiftComplex() {
    int length = return12();
    byte[] byteArray = new byte[4];
    byteArray[0] = (byte) (length >>> 24);
    byteArray[1] = (byte) (length >>> 16);
    byteArray[2] = (byte) (length >>> 8);
    byteArray[3] = (byte) length;
  }

  void testWrite64(long x) {
    write32((int) (x >>> 32));
  }

  void testWrite64() {
    long myLong = Long.MAX_VALUE;
    int z = (int) (myLong >>> 32);
    int myInt = 2;
    short w = (short) (myInt >>> 16);
  }

  int return12() {
    return 12;
  }

  void write32(int x) {}
}
