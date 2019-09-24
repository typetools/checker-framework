import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.framework.qual.PolyAll;

class TestPolyAll {
    @PolyAll String eqRef(@PolyAll String p) {
        return p;
    }

    void fooRef(@Interned String p) {
        p = eqRef(p);
    }

    @PolyAll String[] eqArr(@PolyAll String[] p) {
        return p;
    }

    void fooArr(@Interned String[] p) {
        p = eqArr(p);
    }

    @Interned @PolyAll String intern(@PolyAll TestPolyAll this) {
        throw new RuntimeException("Doesn't matter");
    }

    void use(TestPolyAll test) {
        @Interned String s = test.intern();
    }
}
