// Test case for Issue 145
// https://code.google.com/p/checker-framework/issues/detail?id=145
class UnionTypes {
    public void TryCatch() {
        try {
            int[] arr = new int[10];
            arr[4] = 1;
        } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException exc) {
            Exception e = exc;
        }
    }
}
