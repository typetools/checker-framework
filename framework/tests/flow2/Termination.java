import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.checkerframework.framework.test.*;
import testlib.util.*;

// various tests for @TerminatesExecution
class Termination {

    @TerminatesExecution
    void exit() {}

    void t1(@Odd String p1, String p2, boolean b1) {
        String l1 = p2;
        if (b1) {
            l1 = p1;
        } else {
            exit();
        }
        @Odd String l3 = l1;
    }

    // Apparently the annotated JDK is not used for this test.
    // (It used to pass when the @TerminatesExecution annotation was in a stub file.)
    // void t2(@Odd String p1, String p2, boolean b1) {
    //     String l1 = p2;
    //     if (b1) {
    //         l1 = p1;
    //     } else {
    //         System.exit(0);
    //     }
    //     @Odd String l3 = l1;
    // }
}
