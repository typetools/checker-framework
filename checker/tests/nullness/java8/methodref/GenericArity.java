
// @skip-test
// TODO: enable when checks for generic arity have been added

import org.checkerframework.checker.nullness.qual.*;

interface GenFunc {
    <T extends @Nullable Number, U extends @Nullable Number> T apply(U u);
}

class TestGenFunc {
    static <V extends @NonNull Number, U extends @NonNull Number> V apply(U u) { throw new RuntimeException("");}
    void context() {
        //:: error: (Override message on generic bounds).
        GenFunc f = TestGenFunc::apply;
    }
}