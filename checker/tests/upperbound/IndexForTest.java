package index;

import org.checkerframework.checker.index.qual.IndexFor;

public class IndexForTest {
    int[] array = {0};

    // Once PR #967 is merged and the UBC is updated to use the expression annotation support,
    // (this).array can be rewritten as just array in both the @IndexFor and in the actual array
    // access.
    // https://github.com/typetools/checker-framework/pull/967

    // I accidentally fixed this particular instance of this problem, at least,
    // while fixing an issue with array.length, when closing issue 34. -Martin
    void test1(@IndexFor("array") int i) {
        int x = array[i];
    }

    void callTest1(int x) {
        //::  error: (argument.type.incompatible)
        test1(0);
        //::  error: (argument.type.incompatible)
        test1(1);
        //::  error: (argument.type.incompatible)
        test1(2);
        //::  error: (argument.type.incompatible)
        test1(array.length);
        test1(array.length - 1);
        if (array.length > x) {
            test1(x);
        }

        if (array.length == x) {
            //::  error: (argument.type.incompatible)
            test1(x);
        }
    }
}
