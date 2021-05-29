public class NestedArrayLengthInference {
  public void doStuff(int r) {

    int[] length16array = new int[16];

    int[] unknownLengthArray = new int[r];

    // CF seems to think that if one array has constant length, all do??
    int[][] myNewArray = new int[][] {unknownLengthArray, length16array};
    int[][] myNewArray2 = new int[][] {length16array, unknownLengthArray};
  }
}
