import org.checkerframework.checker.signedness.qual.*;

public class LowerUpperBound {

  public void LowerUpperBoundTest(
      @UnknownSignedness int unknown,
      @Unsigned int unsigned,
      @Signed int signed,
      @SignednessGlb int constant) {

    @UnknownSignedness int unkTest;
    @Unsigned int unsTest;
    @Signed int sinTest;
    @SignednessGlb int conTest;
    @SignednessBottom int botTest;

    unkTest = unknown + unknown;

    // :: error: (assignment.type.incompatible)
    sinTest = unknown + unknown;

    unkTest = unknown + signed;

    // :: error: (assignment.type.incompatible)
    sinTest = unknown + signed;

    sinTest = signed + signed;

    // :: error: (assignment.type.incompatible)
    conTest = signed + signed;

    sinTest = signed + constant;

    // :: error: (assignment.type.incompatible)
    conTest = signed + constant;
  }
}
