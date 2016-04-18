import org.checkerframework.checker.unsignedness.qual.*;

public class Operations {

    public void DivModTest(@Unsigned int unsigned) {

        @UnknownSignedness int testRes;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        testRes = unsigned / 1;

        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        testRes = 1 / unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        testRes = unsigned % 1;

        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        testRes = 1 % unsigned;
    }

    public void SignedRightShiftTest(@Unsigned int unsigned) {

        @UnknownSignedness int testRes;

        //:: error: (binary.operation.shift.signed.type.incompatible)
        testRes = unsigned >> 1;
    }

    public void UnsignedRightShiftTest(@Signed int signed) {

        @UnknownSignedness int testRes;

        //:: error: (binary.operation.shift.unsigned.type.incompatible)
        testRes = signed >>> 1;
    }

    public void BinaryOperationTest(@Unsigned int unsigned, @Signed int signed) {

        @UnknownSignedness int testRes;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned * signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed * unsigned;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned + signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed + unsigned;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned - signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed - unsigned;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned & signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed & unsigned;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned ^ signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed ^ unsigned;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned | signed;

        //:: error: (binary.operation.type.incompatible.mixed.unsignedrhs)
        testRes = signed | unsigned;
    }
}
