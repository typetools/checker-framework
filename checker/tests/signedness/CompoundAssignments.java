import org.checkerframework.checker.signedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(
            @Unsigned int unsigned,
            @UnknownSignedness int unknown,
            @SignednessGlb int constant,
            @Unsigned Integer boxUnsigned,
            @UnknownSignedness Integer boxUnknown,
            @SignednessGlb Integer boxConstant) {

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= unsigned;
        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown /= boxUnsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned /= constant;
        // :: error: (compound.assignment.unsigned.variable)
        boxUnsigned /= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= unsigned;
        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown %= boxUnsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned %= constant;
        // :: error: (compound.assignment.unsigned.variable)
        boxUnsigned %= boxConstant;
    }

    public void SignedRightShiftTest(
            @Unsigned int unsigned,
            @SignednessGlb int constant,
            @Unsigned Integer boxUnsigned,
            @SignednessGlb Integer boxConstant) {

        // :: error: (compound.assignment.shift.signed)
        unsigned >>= constant;
        // :: error: (compound.assignment.shift.signed)
        boxUnsigned >>= boxConstant;
    }

    public void UnsignedRightShiftTest(
            @Signed int signed,
            @SignednessGlb int constant,
            @Signed Integer boxSigned,
            @SignednessGlb Integer boxConstant) {

        // :: error: (compound.assignment.shift.unsigned)
        signed >>>= constant;
        // :: error: (compound.assignment.shift.unsigned)
        boxSigned >>>= boxConstant;
    }

    public void mixedTest(
            @Unsigned int unsigned,
            @Signed int signed,
            @Unsigned Integer boxUnsigned,
            @Signed Integer boxSigned) {

        // :: error: (compound.assignment.mixed.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        unsigned += signed;
        // :: error: (compound.assignment.mixed.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxUnsigned += boxSigned;

        // :: error: (compound.assignment.mixed.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        signed += unsigned;
        // :: error: (compound.assignment.mixed.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxSigned += boxUnsigned;
    }
}
