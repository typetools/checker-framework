// test case for issue 43: https://github.com/kelloggm/checker-framework/issues/43

// @skip-test until fixed

class Pilot2HalfLength {
    private static int[] getFirstHalf(int[] array) {
        int[] firstHalf = new int[array.length / 2];
        for (int i = 0; i < firstHalf.length; i++) {
            firstHalf[i] = array[i];
        }
        return firstHalf;
    }
}
