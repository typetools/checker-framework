package lessthan;

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

// Test for LessThanChecker
public class LessThanValue {

    void subtyping(int x, int y, @LessThan({"#1", "#2"}) int a, @LessThan("#1") int b) {
        @LessThan("x")
        int q = a;
        // :: error: (assignment.type.incompatible)
        @LessThan({"x", "y"})
        int r = b;
    }

    public static boolean flag;

    void lub(int x, int y, @LessThan({"#1", "#2"}) int a, @LessThan("#1") int b) {
        @LessThan("x")
        int r = flag ? a : b;
        // :: error: (assignment.type.incompatible)
        @LessThan({"x", "y"})
        int s = flag ? a : b;
    }

    void transitive(int a, int b, int c) {
        if (a < b) {
            if (b < c) {
                // Not implemented
                // :: error: (assignment.type.incompatible)
                @LessThan("c")
                int x = a;
            }
        }
    }

    void methodStrict(@LessThan("#2") @NonNegative int start, int end) {
        @NonNegative int x = end - start - 1;
        @Positive int y = end - start;
    }

    @NonNegative int method(@LessThan("#2 - 1") @NonNegative int start, int end) {
        return end - start;
    }

    public void setMaximumItemCount(int maximum) {
        if (maximum < 0) {
            throw new IllegalArgumentException("Negative 'maximum' argument.");
        }
        int count = getCount();
        if (count > maximum) {
            @Positive int y = count - maximum;
            @NonNegative int deleteIndex = count - maximum - 1;
        }
    }

    int getCount() {
        throw new RuntimeException();
    }

    void method(@NonNegative int m) {
        boolean[] has_modulus = new boolean[m];
        @LessThan("m")
        int x = foo(m);
        @IndexFor("has_modulus") int rem = foo(m);
    }

    @LessThan("#1")
    @NonNegative int foo(int in) {
        throw new RuntimeException();
    }
}
