class Reassignment {
    void test(int[] arr, int i) {
	if ( i > 0 && i < arr.length) {
	    arr = new int[0];
	    //DISABLED:: warning: (array.access.unsafe.high)
	    int j = arr[i];
	}
    }

}
