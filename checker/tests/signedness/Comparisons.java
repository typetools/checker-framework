import org.checkerframework.checker.signedness.qual.*;

public class Comparisons {

    public void ComparisonTest(@Unsigned int unsigned, @UnknownSignedness int unknown) {

        boolean testRes;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned < unknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown < unsigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned <= unknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown <= unsigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned > unknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown > unsigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned >= unknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown >= unsigned;
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
