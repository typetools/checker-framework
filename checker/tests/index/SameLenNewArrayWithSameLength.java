import org.checkerframework.checker.index.qual.*;

class SameLenNewArrayWithSameLength {
    public void m1(int[] a) {
        int @SameLen("a") [] b = new int[a.length]; // index checker warning
    }
}
