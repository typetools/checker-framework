import polyall.quals.*;
import polyall.quals.H1Invalid;

class ExtraClass {}

class TestMultipleClasses {
    // :: error: (polyall.h1invalid.forbidden)
    @H1Invalid Object getH1Invalid() {
        // :: error: (polyall.h1invalid.forbidden)
        return new @H1Invalid Object();
    }
}
