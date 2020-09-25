import org.checkerframework.common.value.qual.BoolVal;

public class RefineBoolean {

    @BoolVal(true) boolean fTrue = true;

    @BoolVal(false) boolean fFalse = false;

    void test1(boolean x) {
        if (x == false) {
            fFalse = x;
        }
    }

    void test2(boolean x) {
        if (false == x) {
            fFalse = x;
        }
    }

    void test3(boolean x) {
        if (x != true) {
            fFalse = x;
        }
    }

    void test4(boolean x) {
        if (true != x) {
            fFalse = x;
        }
    }

    void test5(boolean x) {
        if (!x) {
            fFalse = x;
        }
    }

    void test6(boolean x) {
        if (x == true) {
            fTrue = x;
        }
    }

    void test7(boolean x) {
        if (true == x) {
            fTrue = x;
        }
    }

    void test8(boolean x) {
        if (false != x) {
            fTrue = x;
        }
    }

    void test9(boolean x) {
        if (x != false) {
            fTrue = x;
        }
    }

    void test10(boolean x) {
        if (x) {
            fTrue = x;
        }
    }
}
