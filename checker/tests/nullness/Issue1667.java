// warning: Interpreting @com.sun.istack.internal.NotNull as a type annotation on an array component type.
// warning: Interpreting @javax.annotation.CheckForNull as a type annotation on an array component type.
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
