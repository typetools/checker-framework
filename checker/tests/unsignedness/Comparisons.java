import org.checkerframework.checker.unsignedness.qual.*;

@skip-test

public class Comparisons {
    void good() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Less than (<) comparison is legal when @Unsigned is not involved
        boolean ltSS = signed < signed;
        boolean ltSC = signed < constant;
        boolean ltSU = signed < unknown;
        boolean ltCS = constant < signed;
        boolean ltCC = constant < constant;
        boolean ltCU = constant < unknown;
        boolean ltUS = unknown < signed;
        boolean ltUC = unknown < constant;
        boolean ltUU = unknown < unknown;

        // Less than or equal (<=) comparison is legal when @Unsigned is not involved
        boolean lteSS = signed <= signed;
        boolean lteSC = signed <= constant;
        boolean lteSU = signed <= unknown;
        boolean lteCS = constant <= signed;
        boolean lteCC = constant <= constant;
        boolean lteCU = constant <= unknown;
        boolean lteUS = unknown <= signed;
        boolean lteUC = unknown <= constant;
        boolean lteUU = unknown <= unknown;

        // Greater than (>) comparison is legal when @Unsigned is not involved
        boolean gtSS = signed > signed;
        boolean gtSC = signed > constant;
        boolean gtSU = signed > unknown;
        boolean gtCS = constant > signed;
        boolean gtCC = constant > constant;
        boolean gtCU = constant > unknown;
        boolean gtUS = unknown > signed;
        boolean gtUC = unknown > constant;
        boolean gtUU = unknown > unknown;

        // Greater than or equal (>=) comparison is legal when @Unsigned is not involved
        boolean gteSS = signed <= signed;
        boolean gteSC = signed <= constant;
        boolean gteSU = signed <= unknown;
        boolean gteCS = constant <= signed;
        boolean gteCC = constant <= constant;
        boolean gteCU = constant <= unknown;
        boolean gteUS = unknown <= signed;
        boolean gteUC = unknown <= constant;
        boolean gteUU = unknown <= unknown;

        // Equal to (==) comparison is legal when it is not the case that one 
        // operand is @Unsigned and the other is @Signed
        boolean etUnUn = unsigned == unsigned;
        boolean etUnC = unsigned == constant;
        boolean etUnU = unsigned == unknown;
        boolean etSS = signed == signed;
        boolean etSC = signed == constant;
        boolean etSU = signed == unknown;
        boolean etCUn = constant == unsigned;
        boolean etCS = constant == signed;
        boolean etCC = constant == constant;
        boolean etCU = constant == unknown;
        boolean etUUn = unknown == unsigned;
        boolean etUS = unknown == signed;
        boolean etUC = unknown == constant;
        boolean etUU = unknown == unknown;

        // Not equal to (!=) comparison is legal when it is not the case that one 
        // operand is @Unsigned and the other is @Signed
        boolean netUnUn = unsigned != unsigned;
        boolean netUnC = unsigned != constant;
        boolean netUnU = unsigned != unknown;
        boolean netSS = signed != signed;
        boolean netSC = signed != constant;
        boolean netSU = signed != unknown;
        boolean netCUn = constant != unsigned;
        boolean netCS = constant != signed;
        boolean netCC = constant != constant;
        boolean netCU = constant != unknown;
        boolean netUUn = unknown != unsigned;
        boolean netUS = unknown != signed;
        boolean netUC = unknown != constant;
        boolean netUU = unknown != unknown;
    }

    void bad() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Less than (<) is illegal when @Unsigned is involved
        //:: error: (binary.comparison.type.incompatible)
        boolean ltUnUn = unsigned < unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltUnS = unsigned < signed;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltUnC = unsigned < constant;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltUnU = unsigned < unknown;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltSUn = signed < unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltCUn = constant < unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean ltUUn = unknown < unsigned;

        // Less than or equal (<=) is illegal when @Unsigned is involved
        //:: error: (binary.comparison.type.incompatible)
        boolean lteUnUn = unsigned <= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteUnS = unsigned <= signed;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteUnC = unsigned <= constant;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteUnU = unsigned <= unknown;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteSUn = signed <= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteCUn = constant <= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean lteUUn = unknown <= unsigned;

        // Greater than (>) is illegal when @Unsigned is involved
        //:: error: (binary.comparison.type.incompatible)
        boolean gtUnUn = unsigned > unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtUnS = unsigned > signed;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtUnC = unsigned > constant;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtUnU = unsigned > unknown;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtSUn = signed > unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtCUn = constant > unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gtUUn = unknown > unsigned;

        // Greater than or equal (>=) is illegal when @Unsigned is involved
        //:: error: (binary.comparison.type.incompatible)
        boolean gteUnUn = unsigned >= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteUnS = unsigned >= signed;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteUnC = unsigned >= constant;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteUnU = unsigned >= unknown;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteSUn = signed >= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteCUn = constant >= unsigned;
        //:: error: (binary.comparison.type.incompatible)
        boolean gteUUn = unknown >= unsigned;

        // Equal to (==) is illegal if one operand is @Unsigned
        // and the other is @Signed
        //:: error: (binary.comparison.type.incompatible.lhs)
        boolean etUnS = unsigned == signed;
        //:: error: (binary.comparison.type.incompatible.rhs)
        boolean etSUn = signed == unsigned;

        // Not equal to (!=) is illegal if one operand is @Unsigned
        // and the other is @Signed
        //:: error: (binary.comparison.type.incompatible.lhs)
        boolean netUnS = unsigned != signed;
        //:: error: (binary.comparison.type.incompatible.rhs)
        boolean netSUn = signed != unsigned;
    }
}
