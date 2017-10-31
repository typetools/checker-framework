import org.checkerframework.framework.test.*;
import testlib.util.*;

class Equal {

    String f1, f2, f3;

    // annotation inference for equality
    void t1(@Odd String p1, String p2) {
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (f1 == p1) {
            @Odd String l2 = f1;
        } else {
            // :: error: (assignment.type.incompatible)
            @Odd String l3 = f1;
        }
    }

    // annotation inference for equality
    void t2(@Odd String p1, String p2) {
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (f1 != p1) {
            // :: error: (assignment.type.incompatible)
            @Odd String l2 = f1;
        } else {
            @Odd String l3 = f1;
        }
    }
}
