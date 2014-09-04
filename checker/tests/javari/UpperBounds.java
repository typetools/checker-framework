import org.checkerframework.checker.javari.qual.*;
import java.util.Date;

class UpperBoundsImpl<T> {
    // an implicit upper bound of a class defaults to readonly
    void testAsClass() {
        UpperBoundsImpl<@Mutable Date> t1 = null;
        UpperBoundsImpl<@ReadOnly Date> t2 = null;
    }

    T instance;
    void testAsUse() {
        UpperBoundsImpl<Date> l = new UpperBoundsImpl<Date>();
        @Mutable Date d = l.instance;
    }
}

class UpperBoundsExpl<T extends Date> {
    // an explicit upper bound of a class defaults to mutable
    void testAsClass() {
        UpperBoundsExpl<@Mutable Date> t1 = null;
        //:: error: (type.argument.type.incompatible)
        UpperBoundsExpl<@ReadOnly Date> t2 = null;
    }

    T instance;
    void testAsUse() {
        UpperBoundsExpl<Date> l = new UpperBoundsExpl<Date>();
        @Mutable Date d = l.instance;
    }
}
