import checkers.nullness.quals.*;

public abstract class PureAndFlow {

    @Nullable String s1;
    @Nullable String s2;

    void nonpure(String s1) {}

    //:: warning: (purity.deterministic.void.method)
    @dataflow.quals.Pure void pure(String s2) {}
    //:: warning: (purity.deterministic.void.method)
    @dataflow.quals.Deterministic void det(String s3) {}

    //:: warning: (purity.deterministic.void.method)
    @dataflow.quals.Pure abstract void abstractpure(String s4);
    //:: warning: (purity.deterministic.void.method)
    @dataflow.quals.Deterministic abstract void abstractdet(String s4);

    void withNonRow() {
        if (s2 != null) {
            nonpure("m");
            //:: error: (argument.type.incompatible)
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
        //:: warning: (purity.deterministic.void.method)
        @dataflow.quals.Pure void ifacepure(String s);
        //:: warning: (purity.deterministic.void.method)
        @dataflow.quals.Deterministic void ifacedet(String s);
    }

    class Cons {
        //:: warning: (purity.deterministic.constructor)
        @dataflow.quals.Pure Cons(String s) {}
        //:: warning: (purity.deterministic.constructor)
        @dataflow.quals.Deterministic Cons(int i) {}
    }
}
