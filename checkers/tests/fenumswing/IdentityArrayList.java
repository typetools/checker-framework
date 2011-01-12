import java.util.Arrays;

/*
* This test case violates an assertion in the compiler.
* It does not depend on the fenum checker, it breaks for any checker.
*/
public class IdentityArrayList {
    public <T> T[] toArray(T[] a) {
        return (T[]) Arrays.copyOf(null, 0, a.getClass());
    }

    public <T> T[] toArray2(T[] a) {
        wc(null, 0, new java.util.LinkedList<T[]>());
        return (T[]) myCopyOf(null, 0, a.getClass());
    }

    public static <T,U> T[] myCopyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        return null;
    }

    public static <T,U> T[] wc(U[] original, int newLength, java.util.List<? extends T[]> arr) {
        return null;
    }
}
