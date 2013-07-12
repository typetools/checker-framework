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
        //:: error: (assignment.type.incompatible)
        rr = rm;    // error
        //:: error: (assignment.type.incompatible)
        rr = rq;    // error
        rr = mr;
        //:: error: (assignment.type.incompatible)
        rr = mm;    // error
        //:: error: (assignment.type.incompatible)
        rr = mq;    // error

        //:: error: (assignment.type.incompatible)
        rm = rr;    // error
        rm = rm;
        //:: error: (assignment.type.incompatible)
        rm = rq;    // error
        //:: error: (assignment.type.incompatible)
        rm = mr;    // error
        rm = mm;
        //:: error: (assignment.type.incompatible)
        rm = mq;    // error

        rq = rr;
        rq = rm;
        rq = rq;
        rq = mr;
        rq = mm;
        rq = mq;

        //:: error: (assignment.type.incompatible)
        mr = rr;    // error
        //:: error: (assignment.type.incompatible)
        mr = rm;    // error
        //:: error: (assignment.type.incompatible)
        mr = rq;    // error
        mr = mr;
        //:: error: (assignment.type.incompatible)
        mr = mm;    // error
        //:: error: (assignment.type.incompatible)
        mr = mq;    // error

        //:: error: (assignment.type.incompatible)
        mm = rr;    // error
        //:: error: (assignment.type.incompatible)
        mm = rm;    // error
        //:: error: (assignment.type.incompatible)
        mm = rq;    // error
        //:: error: (assignment.type.incompatible)
        mm = mr;    // error
        mm = mm;
        //:: error: (assignment.type.incompatible)
        mm = mq;    // error

        //:: error: (assignment.type.incompatible)
        mq = rr;    // error
        //:: error: (assignment.type.incompatible)
        mq = rm;    // error
        //:: error: (assignment.type.incompatible)
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
