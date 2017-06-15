import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

class Issue20 {
    // An issue with LUB that results in losing information when unifying.
    final int[] a, b;

    public Issue20() {
        a = new int[0];
        b = new int[0];
    }

    @LTEqLengthOf("a") int test(@LTLengthOf("a") int i, @LTEqLengthOf({"a", "b"}) int j, boolean flag) {
        return flag ? i : j;
    }
}
