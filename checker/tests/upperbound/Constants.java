
class Constants {

    void test() {
        int[] arr = {1, 2, 3};
        int i = arr[1];
        //:: warning: (array.access.unsafe.high)
        int j = arr[3];
    }
}
