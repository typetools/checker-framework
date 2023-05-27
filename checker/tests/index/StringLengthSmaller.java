// A subset of StringLength.java.

import java.util.Random;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;

public class StringLengthSmaller {

  double d() {
    return 1.0;
  }

  void testRandomMultiply(@MinLen(1) String s, Random r) {
    @LTLengthOf("s") int i = (int) (Math.random() * s.length());
  }
}
