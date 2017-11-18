// Test case for issue #951:
// https://github.com/typetools/checker-framework/issues/951

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue951 {

    @Pure
    public static int min(int[] a) {
        if (a.length == 0) {
            throw new ArrayIndexOutOfBoundsException("Empty array passed to min(int[])");
        }
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = min(result, a[i]);
        }
        return result;
    }

    @Pure
    public static int arbitraryExceptionArg() {
        // :: error: (purity.not.deterministic.not.sideeffectfree.call)
        throw new ArrayIndexOutOfBoundsException("" + arbitraryMethod());
    }

    @Pure
    public static int sefExceptionArg() {
        // The method is safe, so this is a false positive warning;
        // in the future the Purity Checker may not issue this warning.
        // :: error: (purity.not.deterministic.call)
        throw new ArrayIndexOutOfBoundsException("" + sefMethod());
    }

    @Pure
    public static int detExceptionArg() {
        // :: error: (purity.not.sideeffectfree.call)
        throw new ArrayIndexOutOfBoundsException("" + detMethod());
    }

    @Pure
    public static int pureExceptionArg(int a, int b) {
        throw new ArrayIndexOutOfBoundsException("" + min(a, b));
    }

    @Pure
    int throwNewWithinTry() {
        for (int i = 0; i < 10; i++) {
            try {
                for (int j = 0; j < 10; j++) {
                    throw new Error();
                }
                // :: error: (purity.not.deterministic.catch)
            } catch (Error e) {
                return -1;
            }
        }
        return 22;
    }

    // Helper methods

    // Not deterministic or side-effect-free
    static int arbitraryMethod() {
        return 22;
    }

    // Not deterministic
    @SideEffectFree
    static int sefMethod() {
        return 22;
    }

    @Deterministic
    // Not side-effect-free
    static int detMethod() {
        return 22;
    }

    @Pure
    static int min(int a, int b) {
        if (a < b) {
            return a;
        } else {
            return b;
        }
    }
}
