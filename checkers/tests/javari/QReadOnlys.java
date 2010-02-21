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
        //:: (type.incompatible)
        mq = rq; // invalid
        //:: (type.incompatible)
        mq = rr; // invalid
        //:: (type.incompatible)
        mq = rm; // invalid

        // mr test
        //:: (type.incompatible)
        mr = mq; // invalid
        mr = mr;
        //:: (type.incompatible)
        mr = mm; // invalid
        //:: (type.incompatible)
        mr = rq; // invalid
        //:: (type.incompatible)
        mr = rr; // invalid
        //:: (type.incompatible)
        mr = rm; // invalid

        // mm test
        //:: (type.incompatible)
        mm = mq; // invalid
        //:: (type.incompatible)
        mm = mr; // invalid
        mm = mm;
        //:: (type.incompatible)
        mm = rq; // invalid
        //:: (type.incompatible)
        mm = rr; // invalid
        //:: (type.incompatible)
        mm = rm; // invalid

        // rq test
        rq = mq;
        rq = mr;
        rq = mm;
        rq = rq;
        rq = rr;
        rq = rm;

        // rr test
        //:: (type.incompatible)
        rr = mq; // invalid
        rr = mr;
        //:: (type.incompatible)
        rr = mm; // invalid
        //:: (type.incompatible)
        rr = rq; // invalid
        rr = rr;
        //:: (type.incompatible)
        rr = rm; // invalid

        // rm test
        //:: (type.incompatible)
        rm = mq; // invalid
        //:: (type.incompatible)
        rm = mr; // invalid
        rm = mm;
        //:: (type.incompatible)
        rm = rq; // invalid
        //:: (type.incompatible)
        rm = rr; // invalid
        rm = rm;
    }

    void testQReadOnly() {
        List<@QReadOnly Date> lst = null;
        @ReadOnly Date roDate = null;
        //:: (type.incompatible)
        lst.add(roDate);  // invalid
    }
}
