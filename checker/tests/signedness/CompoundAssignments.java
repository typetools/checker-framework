import org.checkerframework.checker.signedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(
            @Unsigned int unsigned,
            @PolySigned int polysigned,
            @UnknownSignedness int unknown,
            @SignednessGlb int constant) {

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= unsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned /= constant;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned /= constant;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= unsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned %= constant;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned %= constant;
    }

    public void SignedRightShiftTest(
            @Unsigned int unsigned, @PolySigned int polysigned, @SignednessGlb int constant) {

        // :: error: (compound.assignment.shift.signed)
        unsigned >>= constant;

        // :: error: (compound.assignment.shift.signed)
        polysigned >>= constant;
    }

    public void UnsignedRightShiftTest(
            @Signed int signed, @PolySigned int polysigned, @SignednessGlb int constant) {

        // :: error: (compound.assignment.shift.unsigned)
        signed >>>= constant;

        // :: error: (compound.assignment.shift.unsigned)
        polysigned >>>= constant;
    }

    public void mixedTest(@Unsigned int unsigned, @Signed int signed) {

        // :: error: (compound.assignment.mixed.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        unsigned += signed;

        // :: error: (compound.assignment.mixed.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        signed += unsigned;
    }
}
