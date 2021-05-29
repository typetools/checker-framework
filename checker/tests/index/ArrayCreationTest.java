import org.checkerframework.checker.index.qual.SameLen;

// Check that creating an array with the length of another
// makes both @SameLen of each other.

public class ArrayCreationTest {
  void test(int[] a, int[] d) {
    int[] b = new int[a.length];
    int @SameLen({"a", "b"}) [] c = b;
  }
}
