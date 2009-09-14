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
        rr = rm;    // error
        rr = rq;    // error
        rr = mr;
        rr = mm;    // error
        rr = mq;    // error

        rm = rr;    // error
        rm = rm;
        rm = rq;    // error
        rm = mr;    // error
        rm = mm;
        rm = mq;    // error

        rq = rr;
        rq = rm;
        rq = rq;
        rq = mr;
        rq = mm;
        rq = mq;

        mr = rr;    // error
        mr = rm;    // error
        mr = rq;    // error
        mr = mr;
        mr = mm;    // error
        mr = mq;    // error

        mm = rr;    // error
        mm = rm;    // error
        mm = rq;    // error
        mm = mr;    // error
        mm = mm;
        mm = mq;    // error

        mq = rr;    // error
        mq = rm;    // error
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
