import org.checkerframework.checker.index.qual.*;

class SameLenNewArrayWithSameLength {
    public void m1(int[] a) {
        int @SameLen("a") [] b = new int[a.length];
    }

    public void m2(int[] a, int @SameLen("#1") [] b) {
        int @SameLen({"a", "b"}) [] c = new int[b.length];
    }
}
