import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.common.value.qual.MinLen;

public class IndexForTest {
    int @MinLen(1) [] array = {0};

    void test1(@IndexFor("array") int i) {
        int x = array[i];
    }

    void callTest1(int x) {
        test1(0);
        //::  error: (argument.type.incompatible)
        test1(1);
        //::  error: (argument.type.incompatible)
        test1(2);
        //::  error: (argument.type.incompatible)
        test1(array.length);

        if (array.length > 0) {
            test1(array.length - 1);
        }

        test1(array.length - 1);

        //::  error: (argument.type.incompatible)
        test1(this.array.length);

        if (array.length > 0) {
            test1(this.array.length - 1);
        }

        test1(this.array.length - 1);

        if (this.array.length > x && x >= 0) {
            test1(x);
        }

        if (array.length == x) {
            //::  error: (argument.type.incompatible)
            test1(x);
        }
    }

    void test2(@IndexFor("this.array") int i) {
        int x = array[i];
    }

    void callTest2(int x) {
        test2(0);
        //::  error: (argument.type.incompatible)
        test2(1);
        //::  error: (argument.type.incompatible)
        test2(2);
        //::  error: (argument.type.incompatible)
        test2(array.length);

        if (array.length > 0) {
            test2(array.length - 1);
        }

        test2(array.length - 1);

        //::  error: (argument.type.incompatible)
        test2(this.array.length);

        if (array.length > 0) {
            test2(this.array.length - 1);
        }

        test2(this.array.length - 1);

        if (array.length == x && x >= 0) {
            //::  error: (argument.type.incompatible)
            test2(x);
        }
    }
}
