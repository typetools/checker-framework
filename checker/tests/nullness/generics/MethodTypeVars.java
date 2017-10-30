import org.checkerframework.checker.nullness.qual.*;

/*
 * This test is based on Issue 93:
 * https://github.com/typetools/checker-framework/issues/93
 */
public class MethodTypeVars {
    void m() {
        // :: error: (type.argument.type.incompatible)
        Object a = A.badMethod(null);
        Object b = A.badMethod(new Object());

        // TODO: false negative. See #979
        //// :: error: (type.argument.type.incompatible)
        A.goodMethod(null);
        A.goodMethod(new Object());
    }
}

class A {
    public static <T extends @NonNull Object> T badMethod(T t) {
        // :: warning: [unchecked] unchecked cast
        return (T) new Object();
    }

    public static <T extends @NonNull Object> void goodMethod(T t) {}
}

class B {
    public <T> void indexOf1(T[] a, @Nullable Object elt) {}
    // This is not valid Java syntax.
    // public void indexOf2(?[] a, @Nullable Object elt) {}

    void call() {
        Integer[] arg = new Integer[] {1, 2, 3, 4};
        indexOf1(arg, new Integer(5));
        // indexOf2(arg, new Integer(5));
    }
}
