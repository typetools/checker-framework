// This test case is for issue 44: https://github.com/kelloggm/checker-framework/issues/44

class Pilot3ArrayCreation {
    void test(int[] firstArray, int[] secondArray[]) {
        int[] newArray = new int[firstArray.length + secondArray.length];
        for (int i = 0; i < firstArray.length; i++) {
            newArray[i] = firstArray[i]; // or newArray[i] = secondArray[i];
        }
    }
}
