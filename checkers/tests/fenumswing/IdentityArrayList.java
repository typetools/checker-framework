import java.util.Arrays;

/*
* This test case violates an assertion in the compiler.
* It does not depend on the fenum checker, it breaks for any checker.
*/
public class IdentityArrayList {
    public <T> T[] toArray(T[] a) {
        // TODO: method type argument inference introduces a wildcard here
        // which generates this warning. How often does this happen and what
        // can be done about it?
        //:: warning: (cast.unsafe)
        return (T[]) Arrays.copyOf(null, 0, a.getClass());
    }

    public <T> T[] toArray2(T[] a) {
        wc(null, 0, new java.util.LinkedList<T[]>());
        // See comment above
        //:: warning: (cast.unsafe)
        return (T[]) myCopyOf(null, 0, a.getClass());
    }

    public static <T,U> T[] myCopyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        return null;
    }

    public static <T,U> T[] wc(U[] original, int newLength, java.util.List<? extends T[]> arr) {
        return null;
    }
}
