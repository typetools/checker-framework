@SuppressWarnings("array.access.unsafe.high")
class LTLDivide {
    int[] test(int[] array) {
        int len = array.length / 2;
        int[] arr = new int[len];
        for (int a = 0; a < len; a++) arr[a] = array[a];
        return arr;
    }
}
