import checkers.javari.quals.*;
import java.util.Date;

class UpperBounds<T extends Date> {
    // check that the upper bound of a class defaults to readonly
    void testAsClass() {
        UpperBounds<@Mutable Date> t1 = null;
        UpperBounds<@ReadOnly Date> t2 = null;
    }

    T instance;
    void testAsUse() {
        UpperBounds<Date> l = new UpperBounds<Date>();
        @Mutable Date d = l.instance;
    }
}

