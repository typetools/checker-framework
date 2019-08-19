import org.checkerframework.checker.signedness.qual.*;

public class Comparisons {

    public void ComparisonTest(
            @Unsigned int unsigned,
            @PolySigned int polysigned,
            @PolySigned Integer boxPolysigned,
            @Unsigned Integer boxUnsigned,
            @UnknownSignedness int unknown,
            @UnknownSignedness Integer boxUnknown) {

        boolean testRes;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned < unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxUnsigned < boxUnknown;

        // :: error: (comparison.unsignedlhs)
        testRes = polysigned < unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxPolysigned < boxUnknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown < unsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown < boxUnsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown < polysigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown < boxPolysigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned <= unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxUnsigned <= boxUnknown;

        // :: error: (comparison.unsignedlhs)
        testRes = polysigned <= unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxPolysigned <= boxUnknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown <= unsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown <= boxUnsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown <= polysigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown <= boxPolysigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned > unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxUnsigned > boxUnknown;

        // :: error: (comparison.unsignedlhs)
        testRes = polysigned > unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxPolysigned > boxUnknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown > unsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown > boxUnsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown > polysigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown > boxPolysigned;

        // :: error: (comparison.unsignedlhs)
        testRes = unsigned >= unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxUnsigned >= boxUnknown;

        // :: error: (comparison.unsignedrhs)
        testRes = unknown >= unsigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown >= boxUnsigned;

        // :: error: (comparison.unsignedlhs)
        testRes = polysigned >= unknown;

        // :: error: (comparison.unsignedlhs)
        testRes = boxPolysigned >= boxUnknown;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown >= boxPolysigned;

        // :: error: (comparison.unsignedrhs)
        testRes = boxUnknown >= boxPolysigned;
    }

    public void EqualsTest(
            @Unsigned int unsigned,
            @Signed int signed,
            @Unsigned Integer boxUnsigned,
            @Signed Integer boxSigned) {

        boolean testRes;

        // :: error: (comparison.mixed.unsignedlhs)
        testRes = unsigned == signed;
        // :: error: (comparison.mixed.unsignedlhs)
        testRes = boxUnsigned == boxSigned;

        // :: error: (comparison.mixed.unsignedrhs)
        testRes = signed == unsigned;
        // :: error: (comparison.mixed.unsignedrhs)
        testRes = boxSigned == boxUnsigned;

        // :: error: (comparison.mixed.unsignedlhs)
        testRes = unsigned != signed;
        // :: error: (comparison.mixed.unsignedlhs)
        testRes = boxUnsigned != boxSigned;

        // :: error: (comparison.mixed.unsignedrhs)
        testRes = signed != unsigned;
        // :: error: (comparison.mixed.unsignedrhs)
        testRes = boxSigned != boxUnsigned;
    }
}
