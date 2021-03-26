import org.checkerframework.checker.signedness.qual.*;

public class BinaryOperations {

  public void DivModTest(
      @Unsigned int unsigned,
      @PolySigned int polysigned,
      @UnknownSignedness int unknown,
      @SignednessGlb int constant) {

    @Unsigned int unsignedresult;
    @UnknownSignedness int unknownresult;

    // :: error: (operation.unsignedrhs)
    unknownresult = unknown / unsigned;

    // :: error: (operation.unsignedlhs)
    unknownresult = unsigned / unknown;

    // :: error: (operation.unsignedlhs)
    unsignedresult = unsigned / constant;

    // :: error: (operation.unsignedrhs)
    unsignedresult = constant / unsigned;

    // :: error: (operation.unsignedrhs)
    unknownresult = unknown / polysigned;

    // :: error: (operation.unsignedlhs)
    unknownresult = polysigned / unknown;

    // :: error: (operation.unsignedlhs)
    unknownresult = polysigned / constant;

    // :: error: (operation.unsignedrhs)
    unknownresult = constant / polysigned;

    // :: error: (operation.unsignedrhs)
    unknownresult = unknown % unsigned;

    // :: error: (operation.unsignedlhs)
    unknownresult = unsigned % unknown;

    // :: error: (operation.unsignedrhs)
    unknownresult = unknown % polysigned;

    // :: error: (operation.unsignedlhs)
    unknownresult = polysigned % unknown;

    // :: error: (operation.unsignedlhs)
    unsignedresult = unsigned % constant;

    // :: error: (operation.unsignedrhs)
    unsignedresult = constant % unsigned;

    // :: error: (operation.unsignedlhs)
    unknownresult = polysigned % constant;

    // :: error: (operation.unsignedrhs)
    unknownresult = constant % polysigned;
  }

  public void SignedRightShiftTest(
      @Unsigned int unsigned,
      @PolySigned int polysigned,
      @UnknownSignedness int unknown,
      @SignednessGlb int constant) {

    @Unsigned int unsignedresult;
    @PolySigned int polysignedresult;
    @UnknownSignedness int unknownresult;
    int result;

    // :: error: (shift.signed)
    unsignedresult = unsigned >> constant;

    result = constant >> unsigned;

    // :: error: (shift.signed)
    polysignedresult = polysigned >> constant;

    result = constant >> polysigned;

    // :: error: (shift.signed)
    unsignedresult = unsigned >> unknown;

    unknownresult = unknown >> unsigned;

    // :: error: (shift.signed)
    polysignedresult = polysigned >> unknown;

    unknownresult = unknown >> polysigned;
  }

  public void UnsignedRightShiftTest(
      @Signed int signed,
      @PolySigned int polysigned,
      @UnknownSignedness int unknown,
      @SignednessGlb int constant) {

    @PolySigned int polysignedresult;
    @UnknownSignedness int unknownresult;
    int result;

    // :: error: (shift.unsigned)
    result = signed >>> constant;

    result = constant >>> signed;

    // :: error: (shift.unsigned)
    result = signed >>> unknown;

    unknownresult = unknown >>> signed;

    // :: error: (shift.unsigned)
    polysignedresult = polysigned >>> constant;

    result = constant >>> polysigned;

    // :: error: (shift.unsigned)
    polysignedresult = polysigned >>> unknown;

    unknownresult = unknown >>> polysigned;
  }

  public void LeftShiftTest(
      @Signed int signed,
      @Unsigned int unsigned,
      @PolySigned int polysigned,
      @UnknownSignedness int unknown,
      @SignednessGlb int constant) {

    @PolySigned int polysignedresult;
    @UnknownSignedness int unknownresult;
    @Unsigned int unsignedresult;
    int result;

    result = signed << constant;

    result = constant << signed;

    result = signed << unknown;

    unknownresult = unknown << signed;

    unsignedresult = unsigned << constant;

    result = constant << unsigned;

    unsignedresult = unsigned << unknown;

    unknownresult = unknown << unsigned;

    polysignedresult = polysigned << constant;

    result = constant << polysigned;

    polysignedresult = polysigned << unknown;

    unknownresult = unknown << polysigned;
  }
}
