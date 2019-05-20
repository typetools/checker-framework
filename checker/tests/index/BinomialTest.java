import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

class BinomialTest {

    static final long @MinLen(1) [] factorials = {1L, 1L, 1L * 2};

    public static long binomial(
            @NonNegative @LTLengthOf("this.factorials") int n,
            @NonNegative @LessThan("#1 + 1") int k) {
        return factorials[k];
    }

    public static void binomial0(@LTLengthOf("this.factorials") int n, @LessThan("#1") int k) {
        @LTLengthOf(value = "factorials", offset = "1") int i = k;
    }

    public static void binomial0Error(@LTLengthOf("this.factorials") int n, @LessThan("#1") int k) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "factorials", offset = "2") int i = k;
    }

    public static void binomial0Weak(@LTLengthOf("this.factorials") int n, @LessThan("#1") int k) {
        @LTLengthOf("factorials") int i = k;
    }

    public static void binomial1(@LTLengthOf("this.factorials") int n, @LessThan("#1 + 1") int k) {
        @LTLengthOf("factorials") int i = k;
    }

    public static void binomial1Error(
            @LTLengthOf("this.factorials") int n, @LessThan("#1 + 1") int k) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "factorials", offset = "1") int i = k;
    }

    public static void binomial2(@LTLengthOf("this.factorials") int n, @LessThan("#1 + 2") int k) {
        @LTLengthOf(value = "factorials", offset = "-1") int i = k;
    }

    public static void binomial2Error(
            @LTLengthOf("this.factorials") int n, @LessThan("#1 + 2") int k) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "factorials", offset = "0") int i = k;
    }

    public static void binomial_1(@LTLengthOf("this.factorials") int n, @LessThan("#1 - 1") int k) {
        @LTLengthOf(value = "factorials", offset = "2") int i = k;
    }

    public static void binomial_1Error(
            @LTLengthOf("this.factorials") int n, @LessThan("#1 - 1") int k) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "factorials", offset = "3") int i = k;
    }

    public static void binomial_2(@LTLengthOf("this.factorials") int n, @LessThan("#1 - 2") int k) {
        @LTLengthOf(value = "factorials", offset = "3") int i = k;
    }

    public static void binomial_2Error(
            @LTLengthOf("this.factorials") int n, @LessThan("#1 - 2") int k) {
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "factorials", offset = "4") int i = k;
    }
}
