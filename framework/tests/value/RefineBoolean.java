import org.checkerframework.common.value.qual.BoolVal;

public class RefineBoolean {

    void test1(boolean x) {
        if (x == false) {
            @BoolVal(false) boolean y = x;
        }
    }

    void test2(boolean x) {
        if (false == x) {
            @BoolVal(false) boolean y = x;
        }
    }

    void test3(boolean x) {
        if (x != true) {
            @BoolVal(false) boolean y = x;
        }
    }

    void test4(boolean x) {
        if (true != x) {
            @BoolVal(false) boolean y = x;
        }
    }

    void test5(boolean x) {
        if (x == true) {
            @BoolVal(true) boolean y = x;
        }
    }

    void test6(boolean x) {
        if (true == x) {
            @BoolVal(true) boolean y = x;
        }
    }

    void test7(boolean x) {
        if (false != x) {
            @BoolVal(true) boolean y = x;
        }
    }

    void test8(boolean x) {
        if (x != false) {
            @BoolVal(true) boolean y = x;
        }
    }
}
