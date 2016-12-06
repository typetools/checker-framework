// This test case is for issue 44: https://github.com/kelloggm/checker-framework/issues/44

// @skip-test until the test is fixed

class Pilot3ArrayCreation {
    void test(int[] firstArray, int[] secondArray[]) {
        int[] newArray = new int[firstArray.length + secondArray.length];
        for (int i = 0; i < firstArray.length; i++) {
            newArray[i] = firstArray[i]; //or newArray[i] = secondArray[i];
        }
    }
}
