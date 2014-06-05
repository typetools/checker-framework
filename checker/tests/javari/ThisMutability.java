import org.checkerframework.checker.javari.qual.*;
import java.util.Date;

/**
 * This is to test that THIS-MUTABLE resolves properly!
 */
public class ThisMutability {
    @Assignable /*this-mutable*/ Date tm;

    void testMutable(@Mutable ThisMutability this) {
        @ReadOnly Date ro = null;
        @Mutable Date m = null;

        ro = tm;
        m = tm;

        //:: error: (assignment.type.incompatible)
        tm = ro;    // error
        tm = m;
    }

    void testReadOnly(@ReadOnly ThisMutability this) {
        @ReadOnly Date ro = null;
        @Mutable Date m = null;


        ro = tm;
        //:: error: (assignment.type.incompatible)
        m = tm;     // error

        // Why should this error be expected?
        // Because this creates a mutable alias
        // to a readonly object!
        // "this" is @ReadOnly, but field is @Assignable
        //TODO:: error: (assignment.type.incompatible)
        tm = ro;    // error
        tm = m;
    }

    void testPolyRead(@PolyRead ThisMutability this) {
        @ReadOnly Date ro = null;
        @Mutable Date m = null;

        ro = tm;
        //:: error: (assignment.type.incompatible)
        m = tm;     // error

        //:: error: (assignment.type.incompatible)
        tm = ro;    // error
        tm = m;
    }
}
