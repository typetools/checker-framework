import org.checkerframework.checker.signedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(
            @Unsigned int unsigned,
            @Unsigned Integer boxUnsigned,
            @PolySigned int polysigned,
            @PolySigned Integer boxPolysigned,
            @UnknownSignedness int unknown,
            @UnknownSignedness Integer boxUnknown,
            @SignednessGlb int constant,
            @SignednessGlb Integer boxConstant) {

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= unsigned;
        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown /= boxUnsigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        unsigned /= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxUnsigned /= boxUnknown;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned /= constant;
        // :: error: (compound.assignment.unsigned.variable)
        boxUnsigned /= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant /= unsigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant /= boxUnsigned;

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown /= boxPolysigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned /= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned /= boxUnknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned /= constant;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned /= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant /= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant /= boxPolysigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant /= unsigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant /= boxUnsigned;

        // :: error: (compound.assignment.unsigned.expression)
        unknown /= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown /= boxPolysigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned /= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned /= boxUnknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned /= constant;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned /= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant /= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant /= boxPolysigned;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= unsigned;

        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown %= boxUnsigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        unsigned %= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxUnsigned %= boxUnknown;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown %= boxPolysigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned %= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned %= boxUnknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        unsigned %= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxUnsigned %= boxUnknown;

        // :: error: (compound.assignment.unsigned.expression)
        unknown %= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        boxUnknown %= boxPolysigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned %= unknown;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned %= boxUnknown;

        // :: error: (compound.assignment.unsigned.variable)
        unsigned %= constant;

        // :: error: (compound.assignment.unsigned.variable)
        boxUnsigned %= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant %= unsigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant %= boxUnsigned;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        polysigned %= constant;

        // :: error: (compound.assignment.unsigned.variable)
        // :: error: (compound.assignment.type.incompatible)
        boxPolysigned %= boxConstant;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        constant %= polysigned;

        // :: error: (compound.assignment.unsigned.expression)
        // :: error: (compound.assignment.type.incompatible)
        boxConstant %= boxPolysigned;
    }

    public void SignedRightShiftTest(
            @Unsigned int unsigned,
            @PolySigned int polysigned,
            @UnknownSignedness int unknown,
            @SignednessGlb int constant) {

        // :: error: (compound.assignment.shift.signed)
        unsigned >>= constant;

        constant >>= unsigned;

        // :: error: (compound.assignment.shift.signed)
        polysigned >>= constant;

        constant >>= polysigned;

        // :: error: (compound.assignment.shift.signed)
        unsigned >>= unknown;

        unknown >>= unsigned;

        // :: error: (compound.assignment.shift.signed)
        polysigned >>= unknown;

        unknown >>= polysigned;
    }

    public void UnsignedRightShiftTest(
            @Signed int signed,
            @PolySigned int polysigned,
            @UnknownSignedness int unknown,
            @SignednessGlb int constant) {

        // :: error: (compound.assignment.shift.unsigned)
        signed >>>= constant;

        constant >>>= signed;

        // :: error: (compound.assignment.shift.unsigned)
        signed >>>= unknown;

        unknown >>>= signed;

        // :: error: (compound.assignment.shift.unsigned)
        polysigned >>>= constant;

        constant >>>= polysigned;

        // :: error: (compound.assignment.shift.unsigned)
        polysigned >>>= unknown;

        unknown >>>= polysigned;
    }

    public void LeftShiftTest(
            @Signed int signed,
            @Unsigned int unsigned,
            @PolySigned int polysigned,
            @UnknownSignedness int unknown,
            @SignednessGlb int constant) {

        signed <<= constant;

        constant <<= signed;

        signed <<= unknown;

        unknown <<= signed;

        unsigned <<= constant;

        constant <<= unsigned;

        unsigned <<= unknown;

        unknown <<= unsigned;

        polysigned <<= constant;

        constant <<= polysigned;

        polysigned <<= unknown;

        unknown <<= polysigned;
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
