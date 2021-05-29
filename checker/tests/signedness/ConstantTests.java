// Tests of constants

import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class ConstantTests {

  @Unsigned int uint_negative_one = (@Unsigned int) -1;

  @Unsigned int u1lit = 0xFFFFFFFE; // unsigned: 2^32 - 2, signed: -2

  void m() {

    int s = -2 / -1;

    @Unsigned int u = -1 / -2;

    int a = -1;
    int b = -2;
    int c = a / b;

    @UnknownSignedness int x = 0xFFFFFFFE / 2;

    int s1 = 0xFFFFFFFE;
    @UnknownSignedness int y = s1 / 2;

    // :: error: (operation.unsignedlhs)
    @UnknownSignedness int z = (uint_negative_one) / -2;

    // :: error: (operation.unsignedlhs)
    @UnknownSignedness int w = u1lit / 2;
  }
}
