import org.checkerframework.checker.signedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(
            @Unsigned int unsigned, @UnknownSignedness int unknown, @Constant int constant) {

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= unsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned /= constant;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= unsigned;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned %= constant;
    }

    public void SignedRightShiftTest(@Unsigned int unsigned, @Constant int constant) {

        // :: error: (compound.assignment.shift.signed)
        unsigned >>= constant;
    }

    public void UnsignedRightShiftTest(@Signed int signed, @Constant int constant) {

        // :: error: (compound.assignment.shift.unsigned)
        signed >>>= constant;
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
