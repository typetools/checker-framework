import org.checkerframework.framework.qual.PolyAll;
import polyall.quals.*;

class Primitive {
    @SuppressWarnings("type.incompatible")
    @H1S2 int o = 4;

    @H1S2 @PolyAll int m(@H1S2 @PolyAll int p) {
        return p;
    }

    void use1(@H1S2 @H2S1 int p) {
        @H1S2 @H2S1 int l = m(p);
    }

    void use2(@H1S2 @H2S2 int p) {
        // :: error: (assignment.type.incompatible)
        @H1S2 @H2S1 int l = m(p);
    }
}
