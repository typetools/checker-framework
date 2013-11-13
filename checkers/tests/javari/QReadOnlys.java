import checkers.javari.quals.*;
import java.util.*;

public class QReadOnlys {

    void testSubtypeAsTypeArgument() {
        List<@QReadOnly Date> mq = null;
        List<@ReadOnly Date> mr = null;
        List<@Mutable Date> mm = null;

        @ReadOnly List<@QReadOnly Date> rq = null;
        @ReadOnly List<@ReadOnly Date> rr = null;
        @ReadOnly List<@Mutable Date> rm = null;

        // mq test
        mq = mq;
        mq = mr;
        mq = mm;
        //:: error: (assignment.type.incompatible)
        mq = rq; // invalid
        //:: error: (assignment.type.incompatible)
        mq = rr; // invalid
        //:: error: (assignment.type.incompatible)
        mq = rm; // invalid

        // mr test
        //:: error: (assignment.type.incompatible)
        mr = mq; // invalid
        mr = mr;
        //:: error: (assignment.type.incompatible)
        mr = mm; // invalid
        //:: error: (assignment.type.incompatible)
        mr = rq; // invalid
        //:: error: (assignment.type.incompatible)
        mr = rr; // invalid
        //:: error: (assignment.type.incompatible)
        mr = rm; // invalid

        // mm test
        //:: error: (assignment.type.incompatible)
        mm = mq; // invalid
        //:: error: (assignment.type.incompatible)
        mm = mr; // invalid
        mm = mm;
        //:: error: (assignment.type.incompatible)
        mm = rq; // invalid
        //:: error: (assignment.type.incompatible)
        mm = rr; // invalid
        //:: error: (assignment.type.incompatible)
        mm = rm; // invalid

        // rq test
        rq = mq;
        rq = mr;
        rq = mm;
        rq = rq;
        rq = rr;
        rq = rm;

        // rr test
        //:: error: (assignment.type.incompatible)
        rr = mq; // invalid
        rr = mr;
        //:: error: (assignment.type.incompatible)
        rr = mm; // invalid
        //:: error: (assignment.type.incompatible)
        rr = rq; // invalid
        rr = rr;
        //:: error: (assignment.type.incompatible)
        rr = rm; // invalid

        // rm test
        //:: error: (assignment.type.incompatible)
        rm = mq; // invalid
        //:: error: (assignment.type.incompatible)
        rm = mr; // invalid
        rm = mm;
        //:: error: (assignment.type.incompatible)
        rm = rq; // invalid
        //:: error: (assignment.type.incompatible)
        rm = rr; // invalid
        rm = rm;
    }

    void testQReadOnly() {
        List<@QReadOnly Date> lst = null;
        @ReadOnly Date roDate = null;
        //:: error: (argument.type.incompatible)
        lst.add(roDate);  // invalid
    }
}
