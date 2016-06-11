import org.checkerframework.checker.unsignedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(@Unsigned int unsigned, @UnknownSignedness int unknown, @Constant int constant) {

        //:: error: (compound.assignment.unsigned.expression)
        unknown /= unsigned;

        //:: error: (compound.assignment.unsigned.variable)
        unsigned /= constant;

        //:: error: (compound.assignment.unsigned.expression)
        unknown %= unsigned;

        //:: error: (compound.assignment.unsigned.variable)
        unsigned %= constant;
    }

    public void SignedRightShiftTest(@Unsigned int unsigned, @Constant int constant) {

        //:: error: (compound.assignment.shift.signed)
        unsigned >>= constant;
    }

    public void UnsignedRightShiftTest(@Signed int signed, @Constant int constant) {

        //:: error: (compound.assignment.shift.unsigned)
        signed >>>= constant;
    }
}
