// A boxed element of an array of primitives is not interned.
public class EnhancedForBoxing {
  void boxArray(int[] array, Integer y) {
    for (Integer x : array) {
      // :: error: (not.interned)
      if (x == y) {}
    }
    Integer z = array[0];
    // :: error: (not.interned)
    if (z == y) {}
  }

  void boxArrayToObject(int[] array, Object y) {
    for (Object x : array) {
      // :: error: (not.interned)
      if (x == y) {}
    }
  }
}
