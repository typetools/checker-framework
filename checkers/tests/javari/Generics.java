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
        //:: (assignment.type.incompatible)
        rr = rm;    // error
        //:: (assignment.type.incompatible)
        rr = rq;    // error
        rr = mr;
        //:: (assignment.type.incompatible)
        rr = mm;    // error
        //:: (assignment.type.incompatible)
        rr = mq;    // error

        //:: (assignment.type.incompatible)
        rm = rr;    // error
        rm = rm;
        //:: (assignment.type.incompatible)
        rm = rq;    // error
        //:: (assignment.type.incompatible)
        rm = mr;    // error
        rm = mm;
        //:: (assignment.type.incompatible)
        rm = mq;    // error

        rq = rr;
        rq = rm;
        rq = rq;
        rq = mr;
        rq = mm;
        rq = mq;

        //:: (assignment.type.incompatible)
        mr = rr;    // error
        //:: (assignment.type.incompatible)
        mr = rm;    // error
        //:: (assignment.type.incompatible)
        mr = rq;    // error
        mr = mr;
        //:: (assignment.type.incompatible)
        mr = mm;    // error
        //:: (assignment.type.incompatible)
        mr = mq;    // error

        //:: (assignment.type.incompatible)
        mm = rr;    // error
        //:: (assignment.type.incompatible)
        mm = rm;    // error
        //:: (assignment.type.incompatible)
        mm = rq;    // error
        //:: (assignment.type.incompatible)
        mm = mr;    // error
        mm = mm;
        //:: (assignment.type.incompatible)
        mm = mq;    // error

        //:: (assignment.type.incompatible)
        mq = rr;    // error
        //:: (assignment.type.incompatible)
        mq = rm;    // error
        //:: (assignment.type.incompatible)
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
