import org.checkerframework.checker.unsignedness.qual.*;

public class Comparisons {

    public void ComparisonTest(@Unsigned int unsigned, @UnknownSignedness int unknown) {

        boolean testRes;

        //:: error: (binary.comparison.type.incompatible.unsignedlhs)
        testRes = unsigned < unknown;

        //:: error: (binary.comparison.type.incompatible.unsignedrhs)
        testRes = unknown < unsigned;

        //:: error: (binary.comparison.type.incompatible.unsignedlhs)
        testRes = unsigned <= unknown;

        //:: error: (binary.comparison.type.incompatible.unsignedrhs)
        testRes = unknown <= unsigned;

        //:: error: (binary.comparison.type.incompatible.unsignedlhs)
        testRes = unsigned > unknown;

        //:: error: (binary.comparison.type.incompatible.unsignedrhs)
        testRes = unknown > unsigned;

        //:: error: (binary.comparison.type.incompatible.unsignedlhs)
        testRes = unsigned >= unknown;

        //:: error: (binary.comparison.type.incompatible.unsignedrhs)
        testRes = unknown >= unsigned;
    }

    public void EqualsTest(@Unsigned int unsigned, @Signed int signed) {

        boolean testRes;

        //:: error: (binary.comparison.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned == signed;

        //:: error: (binary.comparison.type.incompatible.mixed.unsignedrhs)
        testRes = signed == unsigned;

        //:: error: (binary.comparison.type.incompatible.mixed.unsignedlhs)
        testRes = unsigned != signed;

        //:: error: (binary.comparison.type.incompatible.mixed.unsignedrhs)
        testRes = signed != unsigned;
    }
}