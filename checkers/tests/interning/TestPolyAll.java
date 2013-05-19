import checkers.quals.*;
import checkers.interning.quals.*;

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