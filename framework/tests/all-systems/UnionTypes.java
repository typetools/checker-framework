// Test case for Issue 145
// https://github.com/typetools/checker-framework/issues/145
public class UnionTypes {
  public void TryCatch() {
    try {
      int[] arr = new int[10];
      arr[4] = 1;
    } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException exc) {
      @SuppressWarnings(
          "confidential") // correctly prevents cast of @UnknownConfidential to @NonConfidential
      // Exception
      Exception e = exc;
    }
  }
}
