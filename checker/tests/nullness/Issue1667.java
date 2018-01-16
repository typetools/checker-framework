import javax.annotation.CheckForNull;

class Issue1667 {
    @com.sun.istack.internal.NotNull Object[] foo(Object[] bar) {
        // :: error: (return.type.incompatible)
        return null;
    }

    @CheckForNull
    Object[] foo2(@CheckForNull Object[] bar) {
        // :: error: (return.type.incompatible)
        return null;
    }

    void test() {
        // :: error: (argument.type.incompatible)
        foo2(null);
    }
}
