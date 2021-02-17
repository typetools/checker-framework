import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class ArrayFlow {

    // array accesses
    void t1(@Odd String a1[], String a2[], @Odd String odd) {
        String l1 = a1[0];

        // :: error: (assignment.type.incompatible)
        @Odd String l2 = a2[0];

        if (a2[0] == odd) {
            @Odd String l3 = a2[0];
        }

        int i = 1;
        a2[i] = odd;
        @Odd String l4 = a2[i];
        i = 2;
        // :: error: (assignment.type.incompatible)
        @Odd String l5 = a2[i];
    }
}
