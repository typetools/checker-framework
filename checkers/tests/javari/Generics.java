import java.util.*;
import checkers.javari.quals.*;

public class Generics {

    void testOneArg() {
        @ReadOnly List<@ReadOnly Object> rr = null;
        @ReadOnly List<@Mutable Object> rm = null;
        @ReadOnly List<@QReadOnly Object> rq = null;

        @Mutable List<@ReadOnly Object> mr = null;
        @Mutable List<@Mutable Object> mm = null;
        @Mutable List<@QReadOnly Object> mq = null;

        // Assignments test
        rr = rr;
        //:: (type.incompatible)
        rr = rm;    // error
        //:: (type.incompatible)
        rr = rq;    // error
        rr = mr;
        //:: (type.incompatible)
        rr = mm;    // error
        //:: (type.incompatible)
        rr = mq;    // error

        //:: (type.incompatible)
        rm = rr;    // error
        rm = rm;
        //:: (type.incompatible)
        rm = rq;    // error
        //:: (type.incompatible)
        rm = mr;    // error
        rm = mm;
        //:: (type.incompatible)
        rm = mq;    // error

        rq = rr;
        rq = rm;
        rq = rq;
        rq = mr;
        rq = mm;
        rq = mq;

        //:: (type.incompatible)
        mr = rr;    // error
        //:: (type.incompatible)
        mr = rm;    // error
        //:: (type.incompatible)
        mr = rq;    // error
        mr = mr;
        //:: (type.incompatible)
        mr = mm;    // error
        //:: (type.incompatible)
        mr = mq;    // error

        //:: (type.incompatible)
        mm = rr;    // error
        //:: (type.incompatible)
        mm = rm;    // error
        //:: (type.incompatible)
        mm = rq;    // error
        //:: (type.incompatible)
        mm = mr;    // error
        mm = mm;
        //:: (type.incompatible)
        mm = mq;    // error

        //:: (type.incompatible)
        mq = rr;    // error
        //:: (type.incompatible)
        mq = rm;    // error
        //:: (type.incompatible)
        mq = rq;    // error
        mq = mr;
        mq = mm;
        mq = mq;

    }

    public static <T, U extends T> void prepend(List<U> eltsToPrepend, List<T> list) {}

    void prependTest() {
        List<Object> list = null; List<Date> eltsList = null;
        prepend(eltsList, list);
    }

    static <T, U extends T> void prepend(List<T> list, U object) {}

    void prependTest1() {
        List<Object> list = null; Date d = null;
        prepend(list, d);
    }
}
