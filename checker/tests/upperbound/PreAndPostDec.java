import org.checkerframework.checker.lowerbound.qual.*;
import org.checkerframework.checker.minlen.qual.*;

// @skip-test until the bug is fixed

public class PreAndPostDec {

    void pre1(int[] args) {
        int ii = 0;
        while ((ii < args.length)) {
            //:: warning: (array.access.unsafe.high)
            int m = args[++ii];
        }
    }

    void pre2(int[] args) {
        int ii = 0;
        while ((ii < args.length)) {
            ii++;
            //:: warning: (array.access.unsafe.high)
            int m = args[ii];
        }
    }

    void post1(int[] args) {
        int ii = 0;
        while ((ii < args.length)) {
            int m = args[ii++];
        }
    }

    void post2(int[] args) {
        int ii = 0;
        while ((ii < args.length)) {
            int m = args[ii];
            ii++;
        }
    }
}
