import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
public class RawMethodInvocation {
    Object a;
    Object b;

    RawMethodInvocation(boolean constructor_inits_a) {
        a = 1;
        init_b();
    }

    @EnsuresNonNull("b")
    void init_b(@UnknownInitialization RawMethodInvocation this) {
        b = 2;
    }

    RawMethodInvocation(int constructor_inits_none) {
        init_ab();
    }

    @EnsuresNonNull({"a", "b"})
    void init_ab(@UnknownInitialization RawMethodInvocation this) {
        a = 1;
        b = 2;
    }

    RawMethodInvocation(long constructor_escapes_raw) {
        a = 1;
        // this call is not valid, this is still raw
        // :: error: (method.invocation.invalid)
        nonRawMethod();
        b = 2;
    }

    void nonRawMethod() {}
}
