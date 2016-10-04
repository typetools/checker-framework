class LengthOfArrayMinusOne {
    void test(int[] arr) {
	//:: warning: (array.access.unsafe.low)
        int i = arr[arr.length - 1];

	if (arr.length > 0) {
	    int j = arr[arr.length - 1];
	}
    }
}
