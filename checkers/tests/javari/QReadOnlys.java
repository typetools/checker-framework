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
        mq = rq; // invalid
        mq = rr; // invalid
        mq = rm; // invalid

        // mr test
        mr = mq; // invalid
        mr = mr;
        mr = mm; // invalid
        mr = rq; // invalid
        mr = rr; // invalid
        mr = rm; // invalid

        // mm test
        mm = mq; // invalid
        mm = mr; // invalid
        mm = mm;
        mm = rq; // invalid
        mm = rr; // invalid
        mm = rm; // invalid

        // rq test
        rq = mq;
        rq = mr;
        rq = mm;
        rq = rq;
        rq = rr;
        rq = rm;

        // rr test
        rr = mq; // invalid
        rr = mr;
        rr = mm; // invalid
        rr = rq; // invalid
        rr = rr;
        rr = rm; // invalid

        // rm test
        rm = mq; // invalid
        rm = mr; // invalid
        rm = mm;
        rm = rq; // invalid
        rm = rr; // invalid
        rm = rm;
    }

    void testQReadOnly() {
        List<@QReadOnly Date> lst = null;
        @ReadOnly Date roDate = null;
        lst.add(roDate);  // invalid
    }
}
