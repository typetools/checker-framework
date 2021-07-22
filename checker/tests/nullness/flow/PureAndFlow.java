import org.checkerframework.checker.nullness.qual.*;

public abstract class PureAndFlow {

    @Nullable String s1;
    @Nullable String s2;

    void nonpure(String s1) {}

    @org.checkerframework.dataflow.qual.Pure
    // :: warning: (purity.deterministic.void.method)
    void pure(String s2) {}

    @org.checkerframework.dataflow.qual.Deterministic
    // :: warning: (purity.deterministic.void.method)
    void det(String s3) {}

    @org.checkerframework.dataflow.qual.Pure
    // :: warning: (purity.deterministic.void.method)
    abstract void abstractpure(String s4);

    @org.checkerframework.dataflow.qual.Deterministic
    // :: warning: (purity.deterministic.void.method)
    abstract void abstractdet(String s4);

    void withNonRow() {
        if (s2 != null) {
            nonpure("m");
            // :: error: (argument.type.incompatible)
            pure(s2);
        }
    }

    void withPure() {
        if (s2 != null) {
            pure("m");
            pure(s2);
        }
    }

    interface IFace {
        @org.checkerframework.dataflow.qual.Pure
        // :: warning: (purity.deterministic.void.method)
        void ifacepure(String s);

        @org.checkerframework.dataflow.qual.Deterministic
        // :: warning: (purity.deterministic.void.method)
        void ifacedet(String s);
    }

    class Cons {
        @org.checkerframework.dataflow.qual.Pure
        // :: warning: (purity.deterministic.constructor)
        Cons(String s) {}

        @org.checkerframework.dataflow.qual.Deterministic
        // :: warning: (purity.deterministic.constructor)
        Cons(int i) {}
    }
}
