import org.checkerframework.common.value.qual.MinLen;

class Constants {

    void test() {
        int @MinLen(3) [] arr = {1, 2, 3};
        int i = arr[1];
        //:: error: (array.access.unsafe.high.constant)
        int j = arr[3];
    }
}
