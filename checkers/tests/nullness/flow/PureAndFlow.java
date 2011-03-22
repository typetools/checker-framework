import checkers.nullness.quals.*;

public class PureAndFlow {

    @Nullable String s1;
    @Nullable String s2;

    void nonpure(String s1) {}
    @Pure void pure(String s2) {}

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
}
