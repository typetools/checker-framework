class Issue21 {
    @SuppressWarnings("array.access.unsafe.high")
    void test(int[] arr, int[] arr2) {
        for (int i = 0; i < arr2.length && i < arr.length; i++) {
            arr[i] = arr2[i];
        }
    }
}
