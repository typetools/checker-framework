// Test case for Issue 1579
// https://github.com/typetools/checker-framework/issues/1579

public class Issue1579 {
    public int[][] method(int[] array1, int[] array2) {
        // Required for crash
        for (int i = 0; i < array1.length; i++) {}
        return new int[][] {array1, array2};
    }
}
