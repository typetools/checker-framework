// Test case for issue #951:
// https://github.com/typetools/checker-framework/issues/951

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue951 {

    @Pure
    public static int min(int[] a) {
        if (a.length == 0) {
            throw new MyExceptionSefConstructor("Empty array passed to min(int[])");
        }
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = min(result, a[i]);
        }
        return result;
    }

    @Pure
    public static int arbitraryExceptionArg1() {
        // :: error: (purity.not.deterministic.not.sideeffectfree.call.method)
        // :: error: (purity.not.sideeffectfree.call.constructor)
        throw new MyException("" + arbitraryMethod());
    }

    @Pure
    public static int arbitraryExceptionArg2() {
        // :: error: (purity.not.deterministic.not.sideeffectfree.call.method)
        throw new MyExceptionSefConstructor("" + arbitraryMethod());
    }

    @Pure
    public static int sefExceptionArg1() {
        // The method is safe, so this is a false positive warning;
        // in the future the Purity Checker may not issue this warning.
        // :: error: (purity.not.deterministic.call.method)
        // :: error: (purity.not.sideeffectfree.call.constructor)
        throw new MyException("" + sefMethod());
    }

    @Pure
    public static int sefExceptionArg2() {
        // The method is safe, so this is a false positive warning;
        // in the future the Purity Checker may not issue this warning.
        // :: error: (purity.not.deterministic.call.method)
        throw new MyExceptionSefConstructor("" + sefMethod());
    }

    @Pure
    public static int detExceptionArg1() {
        // :: error: (purity.not.sideeffectfree.call.method)
        // :: error: (purity.not.sideeffectfree.call.constructor)
        throw new MyException("" + detMethod());
    }

    @Pure
    public static int detExceptionArg2() {
        // :: error: (purity.not.sideeffectfree.call.method)
        throw new MyExceptionSefConstructor("" + detMethod());
    }

    @Pure
    public static int pureExceptionArg1(int a, int b) {
        // :: error: (purity.not.sideeffectfree.call.constructor)
        throw new MyException("" + min(a, b));
    }

    @Pure
    public static int pureExceptionArg2(int a, int b) {
        throw new MyExceptionSefConstructor("" + min(a, b));
    }

    @Pure
    int throwNewWithinTry() {
        for (int i = 0; i < 10; i++) {
            try {
                for (int j = 0; j < 10; j++) {
                    throw new MyExceptionSefConstructor("foo");
                }
                // :: error: (purity.not.deterministic.catch)
            } catch (MyExceptionSefConstructor e) {
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

    // Constructors

    static class MyException extends Error {
        // Not side-effect-free
        MyException(String message) {}
    }

    static class MyExceptionSefConstructor extends Error {
        // Side-effect-free
        @SuppressWarnings("purity.not.sideeffectfree.call") // until java.util.Error is annotated
        @SideEffectFree
        MyExceptionSefConstructor(String message) {}
    }
}
