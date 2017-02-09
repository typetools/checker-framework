import org.checkerframework.checker.index.qual.IndexFor;

public class IndexForTest {
    int[] array = {0};

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
        test1(array.length - 1);
        //::  error: (argument.type.incompatible)
        test1(this.array.length);
        test1(this.array.length - 1);
        if (this.array.length > x) {
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
        test2(array.length - 1);
        //::  error: (argument.type.incompatible)
        test2(this.array.length);
        test2(this.array.length - 1);
        if (this.array.length > x) {
            test2(x);
        }

        if (array.length == x) {
            //::  error: (argument.type.incompatible)
            test2(x);
        }
    }
}
