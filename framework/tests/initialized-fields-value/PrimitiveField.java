import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;

public class PrimitiveField {
    boolean b;
    byte by;
    char c;
    int i;
    short s;
    float f;
    double d;
    long l;

    @BoolVal(false) boolean b2;

    @IntVal(0) byte by2;

    @IntVal(0) char c2;

    @IntVal(0) int i2;

    @IntVal(0) short s2;

    @DoubleVal(0.0) float f2;

    @DoubleVal(0.0) double d2;

    @IntVal(0) long l2;

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk1 {
        @BoolVal(true) boolean b2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk2 {
        @IntVal(1) byte by2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk3 {
        @IntVal(1) char c2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk4 {
        @IntVal(1) int i2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk5 {
        @IntVal(1) short s2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk6 {
        @DoubleVal(1.0) float f2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk7 {
        @DoubleVal(1.0) double d2;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    static class InitValueNotOk8 {
        @IntVal(2) long l2;
    }
}
