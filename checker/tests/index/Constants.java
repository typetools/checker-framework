import org.checkerframework.common.value.qual.*;

class Constants {

    void test() {
        int @MinLen(3) [] arr = {1, 2, 3};
        int i = arr[1];
        //:: error: (array.access.unsafe.high)
        int j = arr[3];
    }
}
