import testchecker.quals.*;
import testchecker.quals.H1Invalid;

class TypeRefinement {
    // :: warning: (cast.unsafe.constructor.invocation)
    @H1Top Object o = new @H1S1 Object();
    // :: error: (testchecker.h1invalid.forbidden) :: warning: (cast.unsafe.constructor.invocation)
    @H1Top Object o2 = new @H1Invalid Object();
    // :: error: (testchecker.h1invalid.forbidden)
    @H1Top Object o3 = getH1Invalid();

    // :: error: (testchecker.h1invalid.forbidden)
    @H1Invalid Object getH1Invalid() {
        // :: error: (testchecker.h1invalid.forbidden) :: warning:
        // (cast.unsafe.constructor.invocation)
        return new @H1Invalid Object();
    }
}
