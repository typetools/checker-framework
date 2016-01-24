import org.checkerframework.checker.unsignedness.qual.*;

@skip-test

public class CompoundAssignments {

    void good() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Division (/) is legal for all combinations without @Unsigned
        @Signed int divSS = 1;
        divSS /= signed;
        @Signed int divSC = 1;
        divSC /= constant;
        @Signed int divSU = 1;
        divSU /= unknown;

        @Constant int divCS = 1;
        divCS /= signed;
        @Constant int divCC = 1;
        divCC /= constant;
        @Constant int divCU = 1;
        divCU /= unknown;

        @UnknownSignedness int divUS = 1;
        divUS /= signed;
        @UnknownSignedness int divUC = 1;
        divUC /= constant;
        @UnknownSignedness int divUU = 1;
        divUU /= unknown;

        // Remainder (%) is legal for all combinations without @Unsigned
        @Signed int modSS = 1;
        modSS %= signed;
        @Signed int modSC = 1;
        modSC %= constant;
        @Signed int modSU = 1;
        modSU %= unknown;

        @Constant int modCS = 1;
        modCS %= signed;
        @Constant int modCC = 1;
        modCC %= constant;
        @Constant int modCU = 1;
        modCU %= unknown;

        @UnknownSignedness int modUS = 1;
        modUS %= signed;
        @UnknownSignedness int modUC = 1;
        modUC %= constant;
        @UnknownSignedness int modUU = 1;
        modUU %= unknown;

        // Signed shift (>>) is legal for any left operator that is not @Unsigned
        @Signed int sshiftS = 1;
        sshiftS >>= 1;
        @Constant int sshiftC = 1;
        sshiftC >>= 1;
        @UnknownSignedness int sshiftU = 1;
        sshiftU >>= 1;

        // Unsigned shift (>>>) is legal for any left operator that is not @Signed
        @Unsigned int ushiftUn = 1;
        ushiftUn >>>= 1;
        @Constant int ushiftC = 1;
        ushiftC >>>= 1;
        @UnknownSignedness int ushiftU = 1;
        ushiftU >>>= 1;

        // Left shift is always legal for any left operator
        @Unsigned int lshiftUn = 1;
        lshiftUn <<= 1;
        @Signed int lshiftS = 1;
        lshiftS <<= 1;
        @Constant int lshiftC = 1;
        lshiftC <<= 1;
        @UnknownSignedness int lshiftU = 1;
        lshiftU <<= 1;

        // All binary operators should be legal if it is not the case
        // that one operator is unsigned and the other is signed
        
        // Multiplication (*)
        @Unsigned int mulUnUn = 1;
        mulUnUn *= unsigned;
        @Unsigned int mulUnC = 1;
        mulUnC *= constant;
        @Unsigned int mulUnU = 1;
        mulUnU *= unknown;

        @Signed int mulSS = 1;
        mulSS *= signed;
        @Signed int mulSC = 1;
        mulSC *= constant;
        @Signed int mulSU = 1;
        mulSU *= unsigned;

        @Constant int mulCUn = 1;
        mulCUn *= unsigned;
        @Constant int mulCS = 1;
        mulCS *= signed;
        @Constant int mulCC = 1;
        mulCC *= constant;
        @Constant int mulCU = 1;
        mulCU *= unsigned;

        @UnknownSignedness int mulUUn = 1;
        mulUUn *= unsigned;
        @UnknownSignedness int mulUS = 1;
        mulUS *= signed;
        @UnknownSignedness int mulUC = 1;
        mulUC *= constant;
        @UnknownSignedness int mulUU = 1;
        mulUU *= unknown;

        // Addition (+)
        @Unsigned int addUnUn = 1;
        addUnUn += unsigned;
        @Unsigned int addUnC = 1;
        addUnC += constant;
        @Unsigned int addUnU = 1;
        addUnU += unknown;

        @Signed int addSS = 1;
        addSS += signed;
        @Signed int addSC = 1;
        addSC += constant;
        @Signed int addSU = 1;
        addSU += unsigned;

        @Constant int addCUn = 1;
        addCUn += unsigned;
        @Constant int addCS = 1;
        addCS += signed;
        @Constant int addCC = 1;
        addCC += constant;
        @Constant int addCU = 1;
        addCU += unsigned;

        @UnknownSignedness int addUUn = 1;
        addUUn += unsigned;
        @UnknownSignedness int addUS = 1;
        addUS += signed;
        @UnknownSignedness int addUC = 1;
        addUC += constant;
        @UnknownSignedness int addUU = 1;
        addUU += unknown;

        // Subtraction (-)
        @Unsigned int subUnUn = 1;
        subUnUn -= unsigned;
        @Unsigned int subUnC = 1;
        subUnC -= constant;
        @Unsigned int subUnU = 1;
        subUnU -= unknown;

        @Signed int subSS = 1;
        subSS -= signed;
        @Signed int subSC = 1;
        subSC -= constant;
        @Signed int subSU = 1;
        subSU -= unsigned;

        @Constant int subCUn = 1;
        subCUn -= unsigned;
        @Constant int subCS = 1;
        subCS -= signed;
        @Constant int subCC = 1;
        subCC -= constant;
        @Constant int subCU = 1;
        subCU -= unsigned;

        @UnknownSignedness int subUUn = 1;
        subUUn -= unsigned;
        @UnknownSignedness int subUS = 1;
        subUS -= signed;
        @UnknownSignedness int subUC = 1;
        subUC -= constant;
        @UnknownSignedness int subUU = 1;
        subUU -= unknown;

        // Bitwise AND (&)
        @Unsigned int andUnUn = 1;
        andUnUn &= unsigned;
        @Unsigned int andUnC = 1;
        andUnC &= constant;
        @Unsigned int andUnU = 1;
        andUnU &= unknown;

        @Signed int andSS = 1;
        andSS &= signed;
        @Signed int andSC = 1;
        andSC &= constant;
        @Signed int andSU = 1;
        andSU &= unsigned;

        @Constant int andCUn = 1;
        andCUn &= unsigned;
        @Constant int andCS = 1;
        andCS &= signed;
        @Constant int andCC = 1;
        andCC &= constant;
        @Constant int andCU = 1;
        andCU &= unsigned;

        @UnknownSignedness int andUUn = 1;
        andUUn &= unsigned;
        @UnknownSignedness int andUS = 1;
        andUS &= signed;
        @UnknownSignedness int andUC = 1;
        andUC &= constant;
        @UnknownSignedness int andUU = 1;
        andUU &= unknown;

        // Bitwise XOR (^)
        @Unsigned int xorUnUn = 1;
        xorUnUn ^= unsigned;
        @Unsigned int xorUnC = 1;
        xorUnC ^= constant;
        @Unsigned int xorUnU = 1;
        xorUnU ^= unknown;

        @Signed int xorSS = 1;
        xorSS ^= signed;
        @Signed int xorSC = 1;
        xorSC ^= constant;
        @Signed int xorSU = 1;
        xorSU ^= unsigned;

        @Constant int xorCUn = 1;
        xorCUn ^= unsigned;
        @Constant int xorCS = 1;
        xorCS ^= signed;
        @Constant int xorCC = 1;
        xorCC ^= constant;
        @Constant int xorCU = 1;
        xorCU ^= unsigned;

        @UnknownSignedness int xorUUn = 1;
        xorUUn ^= unsigned;
        @UnknownSignedness int xorUS = 1;
        xorUS ^= signed;
        @UnknownSignedness int xorUC = 1;
        xorUC ^= constant;
        @UnknownSignedness int xorUU = 1;
        xorUU ^= unknown;

        // Bitwise or (|)
        @Unsigned int orUnUn = 1;
        orUnUn |= unsigned;
        @Unsigned int orUnC = 1;
        orUnC |= constant;
        @Unsigned int orUnU = 1;
        orUnU |= unknown;

        @Signed int orSS = 1;
        orSS |= signed;
        @Signed int orSC = 1;
        orSC |= constant;
        @Signed int orSU = 1;
        orSU |= unsigned;

        @Constant int orCUn = 1;
        orCUn |= unsigned;
        @Constant int orCS = 1;
        orCS |= signed;
        @Constant int orCC = 1;
        orCC |= constant;
        @Constant int orCU = 1;
        orCU |= unsigned;

        @UnknownSignedness int orUUn = 1;
        orUUn |= unsigned;
        @UnknownSignedness int orUS = 1;
        orUS |= signed;
        @UnknownSignedness int orUC = 1;
        orUC |= constant;
        @UnknownSignedness int orUU = 1;
        orUU |= unknown;
    }

    void bad() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Division (/) with @Unsigned operands are illegal

        @Unsigned int divUnUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        divUnUn /= unsigned;
        @Unsigned int divUnS = 1;
        //:: error: (compoundassignment.type.incompatible)
        divUnS /= signed;
        @Unsigned int divUnC = 1;
        //:: error: (compoundassignment.type.incompatible)
        divUnC /= constant;
        @Unsigned int divUnU = 1;
        //:: error: (compoundassignment.type.incompatible)
        divUnU /= unknown;
        @Signed int divSUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        divSUn /= unsigned;
        @Constant int divCUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        divCUn /= unsigned;
        @UnknownSignedness int divUUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        divUUn /= unsigned;

        // Remainder (%) with @Unsigned operators are illegal

        @Unsigned int modUnUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        modUnUn %= unsigned;
        @Unsigned int modUnS = 1;
        //:: error: (compoundassignment.type.incompatible)
        modUnS %= signed;
        @Unsigned int modUnC = 1;
        //:: error: (compoundassignment.type.incompatible)
        modUnC %= constant;
        @Unsigned int modUnU = 1;
        //:: error: (compoundassignment.type.incompatible)
        modUnU %= unknown;
        @Signed int modSUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        modSUn %= unsigned;
        @Constant int modCUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        modCUn %= unsigned;
        @UnknownSignedness int modUUn = 1;
        //:: error: (compoundassignment.type.incompatible)
        modUUn %= unsigned;

        // Signed right shift (>>) is illegal with left @Unsigned operand
        @Unsigned int sshiftUn = 1;
        //:: error: (compoundassignment.shift.type.incompatible)
        sshiftUn >>= 1;

        // Unsigned right shift (>>>) is illegal with left @Signed operand
        @Signed int ushiftS = 1;
        //:: error: (compoundassignment.shift.type.incompatible)
        ushiftS >>>= 1;

        // All other operators with one @Unsigned and one @Signed are illegal

        @Unsigned int mulUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        mulUnS *= signed;
        @Signed int mulSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        mulSUn *= unsigned;

        @Unsigned int addUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        addUnS += signed;
        @Signed int addSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        addSUn += unsigned;

        @Unsigned int subUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        subUnS -= signed;
        @Signed int subSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        subSUn -= unsigned;

        @Unsigned int andUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        andUnS &= signed;
        @Signed int andSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        andSUn &= unsigned;

        @Unsigned int xorUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        xorUnS ^= signed;
        @Signed int xorSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        xorSUn ^= unsigned;

        @Unsigned int orUnS = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedlhs)
        orUnS |= signed;
        @Signed int orSUn = 1;
        //:: error: (compoundassignment.type.incompatible.unsignedrhs)
        orSUn |= unsigned;
    }
}
