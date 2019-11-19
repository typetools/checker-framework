import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

@org.checkerframework.framework.qual.DefaultQualifier(Nullable.class)
class RawMethodInvocation {
    @NonNull String a;
    @NonNull String b;

    RawMethodInvocation(boolean constructor_inits_a) {
        a = "";
        init_b();
    }

    @EnsuresNonNull("b")
    void init_b(@UnknownInitialization RawMethodInvocation this) {
        b = "";
    }

    // :: error: (initialization.fields.uninitialized)
    RawMethodInvocation(Byte constructor_inits_b) {
        init_b();
    }

    // :: error: (initialization.fields.uninitialized)
    RawMethodInvocation(byte constructor_inits_b) {
        b = "";
        init_b();
    }

    RawMethodInvocation(int constructor_inits_none) {
        init_ab();
    }

    @EnsuresNonNull({"a", "b"})
    void init_ab(@UnknownInitialization RawMethodInvocation this) {
        a = "";
        b = "";
    }

    RawMethodInvocation(long constructor_escapes_raw) {
        a = "";
        // :: error: (method.invocation.invalid)
        nonRawMethod();
        b = "";
    }

    void nonRawMethod() {}
}
