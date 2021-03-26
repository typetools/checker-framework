import org.checkerframework.checker.signedness.qual.*;

public class Comparisons {

  public void ComparisonTest(
      @Unsigned int unsigned, @PolySigned int polysigned, @UnknownSignedness int unknown) {

    boolean testRes;

    // :: error: (comparison.unsignedlhs)
    testRes = unsigned < unknown;

    // :: error: (comparison.unsignedlhs)
    testRes = polysigned < unknown;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown < unsigned;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown < polysigned;

    // :: error: (comparison.unsignedlhs)
    testRes = unsigned <= unknown;

    // :: error: (comparison.unsignedlhs)
    testRes = polysigned <= unknown;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown <= unsigned;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown <= polysigned;

    // :: error: (comparison.unsignedlhs)
    testRes = unsigned > unknown;

    // :: error: (comparison.unsignedlhs)
    testRes = polysigned > unknown;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown > unsigned;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown > polysigned;

    // :: error: (comparison.unsignedlhs)
    testRes = unsigned >= unknown;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown >= unsigned;

    // :: error: (comparison.unsignedlhs)
    testRes = polysigned >= unknown;

    // :: error: (comparison.unsignedrhs)
    testRes = unknown >= polysigned;
  }

  public void EqualsTest(@Unsigned int unsigned, @Signed int signed) {

    boolean testRes;

    // :: error: (comparison.mixed.unsignedlhs)
    testRes = unsigned == signed;

    // :: error: (comparison.mixed.unsignedrhs)
    testRes = signed == unsigned;

    // :: error: (comparison.mixed.unsignedlhs)
    testRes = unsigned != signed;

    // :: error: (comparison.mixed.unsignedrhs)
    testRes = signed != unsigned;
  }
}
