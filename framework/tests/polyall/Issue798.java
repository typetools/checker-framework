// Test case for Issue 798
// https://github.com/typetools/checker-framework/issues/798

import polyall.quals.*;

public class Issue798 {
    void test1(String format, @H1S1 Object @H1S2 ... args) {
        String.format(format, args);
    }

    void test2(String format, @H1S1 Object @H1S1 ... args) {
        // :: error: (argument.type.incompatible)
        String.format(format, args);
    }

    void test3(String format, @H1S2 Object @H1S2 ... args) {
        // :: error: (argument.type.incompatible)
        String.format(format, args);
    }
}
