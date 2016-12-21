import org.checkerframework.checker.samelen.qual.*;

// Check that creating an array with the length of another
// makes both @SameLen of each other.

class ArrayCreation {
    void test(int[] a, int[] d) {
        int[] b = new int[a.length];
        int @SameLen({"a", "b"}) [] c = b;
        int @SameLen("d") [] e = new int[d.length];
    }
}
