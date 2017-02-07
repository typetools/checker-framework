import org.checkerframework.checker.index.qual.*;

class SameLenSimpleCase {
    public void compare(int[] a1, int[] a2) {
        if (a1.length != a2.length) {
            return;
        }
        int @SameLen("a1") [] x = a2;
        int @SameLen("a2") [] r = a1;
    }

    public void compare2(int[] a1, int[] a2) {
        if (a1.length != a2.length) {
            return;
        } else {
            int @SameLen("a1") [] x = a2;
            int @SameLen("a2") [] r = a1;
        }
    }

    public void compare3(int[] a1, int[] a2) {
        if (a1.length == a2.length) {
            int @SameLen("a1") [] x = a2;
            int @SameLen("a2") [] r = a1;
        }
    }
}
