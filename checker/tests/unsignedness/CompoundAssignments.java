import org.checkerframework.checker.unsignedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(@Unsigned int unsigned, @UnknownSignedness int unknown, @Constant int constant) {

        //:: error: (compound.assignment.type.incompatible.unsigned.expression)
        unknown /= unsigned;

        //:: error: (compound.assignment.type.incompatible.unsigned.variable)
        unsigned /= constant;

        //:: error: (compound.assignment.type.incompatible.unsigned.expression)
        unknown %= unsigned;

        //:: error: (compound.assignment.type.incompatible.unsigned.variable)
        unsigned %= constant;
    }

    public void SignedRightShiftTest(@Unsigned int unsigned, @Constant int constant) {

        //:: error: (compound.assignment.shift.signed.type.incompatible)
        unsigned >>= constant;
    }

    public void UnsignedRightShiftTest(@Signed int signed, @Constant int constant) {

        //:: error: (compound.assignment.shift.unsigned.type.incompatible)
        signed >>>= constant;
    }
}
