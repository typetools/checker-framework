import org.checkerframework.checker.index.qual.*;

class TypeArrayLengthWithSameLen {
    @LTEqLengthOf({"#1", "#2", "#3"}) Integer test(int @SameLen("#2") [] a, int @SameLen("#1") [] b, int[] c) {
        if (a.length == c.length) {
            return b.length;
        } else {
            return null;
        }
    }
}
