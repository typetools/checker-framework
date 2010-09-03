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
        //:: (assignment.type.incompatible)
        mq = rq; // invalid
        //:: (assignment.type.incompatible)
        mq = rr; // invalid
        //:: (assignment.type.incompatible)
        mq = rm; // invalid

        // mr test
        //:: (assignment.type.incompatible)
        mr = mq; // invalid
        mr = mr;
        //:: (assignment.type.incompatible)
        mr = mm; // invalid
        //:: (assignment.type.incompatible)
        mr = rq; // invalid
        //:: (assignment.type.incompatible)
        mr = rr; // invalid
        //:: (assignment.type.incompatible)
        mr = rm; // invalid

        // mm test
        //:: (assignment.type.incompatible)
        mm = mq; // invalid
        //:: (assignment.type.incompatible)
        mm = mr; // invalid
        mm = mm;
        //:: (assignment.type.incompatible)
        mm = rq; // invalid
        //:: (assignment.type.incompatible)
        mm = rr; // invalid
        //:: (assignment.type.incompatible)
        mm = rm; // invalid

        // rq test
        rq = mq;
        rq = mr;
        rq = mm;
        rq = rq;
        rq = rr;
        rq = rm;

        // rr test
        //:: (assignment.type.incompatible)
        rr = mq; // invalid
        rr = mr;
        //:: (assignment.type.incompatible)
        rr = mm; // invalid
        //:: (assignment.type.incompatible)
        rr = rq; // invalid
        rr = rr;
        //:: (assignment.type.incompatible)
        rr = rm; // invalid

        // rm test
        //:: (assignment.type.incompatible)
        rm = mq; // invalid
        //:: (assignment.type.incompatible)
        rm = mr; // invalid
        rm = mm;
        //:: (assignment.type.incompatible)
        rm = rq; // invalid
        //:: (assignment.type.incompatible)
        rm = rr; // invalid
        rm = rm;
    }

    void testQReadOnly() {
        List<@QReadOnly Date> lst = null;
        @ReadOnly Date roDate = null;
        //:: (argument.type.incompatible)
        lst.add(roDate);  // invalid
    }
}
