import checkers.quals.PolyAll;
import polyall.quals.*;

class TestPolyAll {
    @H1S2 Object o = null;

    @H1S2 @PolyAll Object m(@H1S2 @PolyAll Object p) {
        return p;
    }
    void use(@H1S2 @H2S1 Object p) {
        @H1S2 @H2S1 Object l = m(p);
    }
}
