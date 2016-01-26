import org.checkerframework.checker.unsignedness.qual.*;

public class CompoundAssignments {

    public void DivModTest(@Unsigned int unsigned, @UnknownSignedness int unknown, @Constant int constant) {

        //:: error: (compound.assignment.type.incompatible)
        unknown /= unsigned;

        //:: error: (compound.assignment.type.incompatible)
        unsigned /= constant;

        //:: error: (compound.assignment.type.incompatible)
        unknown %= unsigned;

        //:: error: (compound.assignment.type.incompatible)
        unsigned %= constant;
    }

    public void SignedRightShiftTest(@Unsigned int unsigned, @Constant int constant) {

        //:: error: (compound.assignment.shift.type.incompatible)
        unsigned >>= constant;
    }

    public void UnsignedRightShiftTest(@Signed int signed, @Constant int constant) {

        //:: error: (compound.assignment.shift.type.incompatible)
        signed >>>= constant;
    }
}
