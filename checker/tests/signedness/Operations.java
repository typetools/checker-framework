import org.checkerframework.checker.signedness.qual.*;

public class Operations {

  public void DivModTest(@Unsigned int unsigned) {

    @UnknownSignedness int testRes;

    // :: error: (operation.unsignedlhs)
    testRes = unsigned / 1;

    // :: error: (operation.unsignedrhs)
    testRes = 1 / unsigned;

    // :: error: (operation.unsignedlhs)
    testRes = unsigned % 1;

    // :: error: (operation.unsignedrhs)
    testRes = 1 % unsigned;
  }

  public void SignedRightShiftTest(@Unsigned int unsigned) {

    @UnknownSignedness int testRes;

    // :: error: (shift.signed)
    testRes = unsigned >> 1;
  }

  public void UnsignedRightShiftTest(@Signed int signed) {

    @UnknownSignedness int testRes;

    // :: error: (shift.unsigned)
    testRes = signed >>> 1;
  }

  public void BinaryOperationTest(@Unsigned int unsigned, @Signed int signed) {

    @UnknownSignedness int testRes;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned * signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed * unsigned;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned + signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed + unsigned;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned - signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed - unsigned;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned & signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed & unsigned;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned ^ signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed ^ unsigned;

    // :: error: (operation.mixed.unsignedlhs)
    testRes = unsigned | signed;

    // :: error: (operation.mixed.unsignedrhs)
    testRes = signed | unsigned;
  }
}
