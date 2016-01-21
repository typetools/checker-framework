import org.checkerframework.checker.unsignedness.qual.*;

public class Operations {

    void good() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Division (/) is legal for all combinations without @Unsigned
        int divSS = signed / signed;
        int divSC = signed / constant;
        int divSU = signed / unknown;

        int divCS = constant / signed;
        int divCC = constant / constant;
        int divCU = constant / unknown;

        int divUS = unknown / signed;
        int divUC = unknown / constant;
        int divUU = unknown / unknown;

        // Remainder (%) is legal for all combinations without @Unsigned
        int modSS = signed % signed;
        int modSC = signed % constant;
        int modSU = signed % unknown;

        int modCS = constant % signed;
        int modCC = constant % constant;
        int modCU = constant % unknown;

        int modUS = unknown % signed;
        int modUC = unknown % constant;
        int modUU = unknown % unknown;

        // Signed shift (>>) is legal for any left operator that is not @Unsigned
        int sshiftS = signed >> 1;
        int sshiftC = constant >> 1;
        int sshiftU = unknown >> 1;

        // Unsigned shift (>>>) is legal for any left operator that is not @Signed
        int ushiftUn = unsigned >>> 1;
        int ushiftC = constant >>> 1;
        int ushiftU = unknown >>> 1;

        // All unary operators are legal
        int postIncUn = unsigned++;
        int postIncS = signed++;
        int postIncC = constant++;
        int postIncU = unknown++;

        int postDecUn = unsigned--;
        int postDecS = signed--;
        int postDecC = constant--;
        int postDecU = unknown--;

        int preIncUn = ++unsigned;
        int preIncS = ++signed;
        int preIncC = ++constant;
        int preIncU = ++unknown;

        int preDecUn = --unsigned;
        int preDecS = --signed;
        int preDecC = --constant;
        int preDecU = --unknown;

        int prePosUn = +unsigned;
        int prePosS = +signed;
        int prePosC = +constant;
        int prePosU = +unknown;

        int preNegUn = -unsigned;
        int preNegS = -signed;
        int preNegC = -constant;
        int preNegU = -unknown;

        int bitNegUn = ~unsigned;
        int bitNegS = ~signed;
        int bitNegC = ~constant;
        int bitNegU = ~unknown;

        // Left shift is always legal for any left operator
        int lshiftUn = unsigned << 1;
        int lshiftS = signed << 1;
        int lshiftC = constant << 1;
        int lshiftU = unknown << 1;

        // All binary operators should be legal if it is not the case
        // that one operator is unsigned and the other is signed
        
        // Multiplication (*)
        int mulUnUn = unsigned * unsigned;
        int mulUnC = unsigned * constant;
        int mulUnU = unsigned * unknown;

        int mulSS = signed * signed;
        int mulSC = signed * constant;
        int mulSU = signed * unsigned;

        int mulCUn = constant * unsigned;
        int mulCS = constant * signed;
        int mulCC = constant * constant;
        int mulCU = constant * unsigned;

        int mulUUn = unknown * unsigned;
        int mulUS = unknown * signed;
        int mulUC = unknown * constant;
        int mulUU = unknown * unknown;

        // Addition (+)
        int addUnUn = unsigned + unsigned;
        int addUnC = unsigned + constant;
        int addUnU = unsigned + unknown;

        int addSS = signed + signed;
        int addSC = signed + constant;
        int addSU = signed + unsigned;

        int addCUn = constant + unsigned;
        int addCS = constant + signed;
        int addCC = constant + constant;
        int addCU = constant + unsigned;

        int addUUn = unknown + unsigned;
        int addUS = unknown + signed;
        int addUC = unknown + constant;
        int addUU = unknown + unknown;

        // Subtraction (-)
        int subUnUn = unsigned - unsigned;
        int subUnC = unsigned - constant;
        int subUnU = unsigned - unknown;

        int subSS = signed - signed;
        int subSC = signed - constant;
        int subSU = signed - unsigned;

        int subCUn = constant - unsigned;
        int subCS = constant - signed;
        int subCC = constant - constant;
        int subCU = constant - unsigned;

        int subUUn = unknown - unsigned;
        int subUS = unknown - signed;
        int subUC = unknown - constant;
        int subUU = unknown - unknown;

        // Bitwise AND (&)
        int andUnUn = unsigned & unsigned;
        int andUnC = unsigned & constant;
        int andUnU = unsigned & unknown;

        int andSS = signed & signed;
        int andSC = signed & constant;
        int andSU = signed & unsigned;

        int andCUn = constant & unsigned;
        int andCS = constant & signed;
        int andCC = constant & constant;
        int andCU = constant & unsigned;

        int andUUn = unknown & unsigned;
        int andUS = unknown & signed;
        int andUC = unknown & constant;
        int andUU = unknown & unknown;

        // Bitwise XOR (^)
        int xorUnUn = unsigned ^ unsigned;
        int xorUnC = unsigned ^ constant;
        int xorUnU = unsigned ^ unknown;

        int xorSS = signed ^ signed;
        int xorSC = signed ^ constant;
        int xorSU = signed ^ unsigned;

        int xorCUn = constant ^ unsigned;
        int xorCS = constant ^ signed;
        int xorCC = constant ^ constant;
        int xorCU = constant ^ unsigned;

        int xorUUn = unknown ^ unsigned;
        int xorUS = unknown ^ signed;
        int xorUC = unknown ^ constant;
        int xorUU = unknown ^ unknown;

        // Bitwise or (|)
        int orUnUn = unsigned | unsigned;
        int orUnC = unsigned | constant;
        int orUnU = unsigned | unknown;

        int orSS = signed | signed;
        int orSC = signed | constant;
        int orSU = signed | unsigned;

        int orCUn = constant | unsigned;
        int orCS = constant | signed;
        int orCC = constant | constant;
        int orCU = constant | unsigned;

        int orUUn = unknown | unsigned;
        int orUS = unknown | signed;
        int orUC = unknown | constant;
        int orUU = unknown | unknown;
    }

    void bad() {
        @Unsigned int unsigned = 1;
        @Signed int signed = 1;
        @Constant int constant = 1;
        @UnknownSignedness int unknown = 1;

        // Division (/) with @Unsigned operators are illegal

        //:: error: (binary.operation.type.incompatible)
        int divUnUn = unsigned / unsigned;
        //:: error: (binary.operation.type.incompatible)
        int divUnS = unsigned / signed;
        //:: error: (binary.operation.type.incompatible)
        int divUnC = unsigned / constant;
        //:: error: (binary.operation.type.incompatible)
        int divUnU = unsigned / unknown;
        //:: error: (binary.operation.type.incompatible)
        int divSUn = signed / unsigned;
        //:: error: (binary.operation.type.incompatible)
        int divCUn = constant / unsigned;
        //:: error: (binary.operation.type.incompatible)
        int divUUn = unknown / unsigned;

        // Remainder (%) with @Unsigned operators are illegal

        //:: error: (binary.operation.type.incompatible)
        int modUnUn = unsigned % unsigned;
        //:: error: (binary.operation.type.incompatible)
        int modUnS = unsigned % signed;
        //:: error: (binary.operation.type.incompatible)
        int modUnC = unsigned % constant;
        //:: error: (binary.operation.type.incompatible)
        int modUnU = unsigned % unknown;
        //:: error: (binary.operation.type.incompatible)
        int modSUn = signed % unsigned;
        //:: error: (binary.operation.type.incompatible)
        int modCUn = constant % unsigned;
        //:: error: (binary.operation.type.incompatible)
        int modUUn = unknown % unsigned;

        // Signed right shift (>>) is illegal with left @Unsigned operand
        //:: error: (binary.operation.shift.type.incompatible)
        int sshiftUn = unsigned >> 1;

        // Unsigned right shift (>>>) is illegal with left @Signed operand
        //:: error: (binary.operation.shift.type.incompatible)
        int ushiftS = signed >>> 1;

        // All other operators with one @Unsigned and one @Signed are illegal

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int mulUnS = unsigned * signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int mulSUn = signed * unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int addUnS = unsigned + signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int addSUn = signed + unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int subUnS = unsigned - signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int subSUn = signed - unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int andUnS = unsigned & signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int andSUn = signed & unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int xorUnS = unsigned ^ signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int xorSUn = signed ^ unsigned;

        //:: error: (binary.operation.type.incompatible.unsignedlhs)
        int orUnS = unsigned | signed;
        //:: error: (binary.operation.type.incompatible.unsignedrhs)
        int orSUn = signed | unsigned;
    }
}
