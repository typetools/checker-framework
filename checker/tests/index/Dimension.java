@SuppressWarnings("lowerbound")
public class Dimension {
  void test(int expr) {
    int[] array = new int[expr];
    // :: error: (array.access.unsafe.high)
    array[expr] = 0;
    array[expr - 1] = 0;
  }

  String[] arrayField = new String[1];

  void test2(int expr) {
    arrayField = new String[expr];
    // :: error: (array.access.unsafe.high)
    this.arrayField[expr] = "";
    this.arrayField[expr - 1] = "";
  }
}
