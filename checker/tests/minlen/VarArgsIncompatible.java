import org.checkerframework.checker.minlen.qual.*;

public class VarArgsIncompatible {

    // The call to help below throws an error. It should be uncommented.

    public static void test(int[] arr) {
        // help(arr);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> void help(T... arr) {}
}
