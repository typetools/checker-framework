import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.interning.qual.*;

class TestPolyAll {
    @PolyAll String eqRef(@PolyAll String p) { return p; }

    void fooRef(@Interned String p) {
        p = eqRef(p);
    }

    @PolyAll String[] eqArr(@PolyAll String[] p) { return p; }

    void fooArr(@Interned String[] p) {
        p = eqArr(p);
    }
}