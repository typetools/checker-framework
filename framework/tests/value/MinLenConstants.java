import org.checkerframework.common.value.qual.*;

class MinLenConstants {

    void test() {
        int @MinLen(3) [] arr = {1, 2, 3};
    }
}
