import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

class Issue1667 {
    @NonNullDecl
    Object[] bar(Object[] bar) {
        // :: error: (return.type.incompatible)
        return null;
    }

    @NonNullDecl
    Object[] foo2(@CheckForNull Object[] bar) {
        // :: error: (return.type.incompatible)
        return null;
    }

    @CheckForNull
    Object[] foo3(@CheckForNull Object[] bar) {
        return null;
    }

    void test() {
        foo2(null);
    }
}
