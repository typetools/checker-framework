import org.checkerframework.checker.interning.qual.Interned;

// A boxed element of an array of primitives is not interned, but an element of an array of
// primitives is interned.
public class EnhancedForBoxing {
  void boxArray(int[] array) {
    for (Integer x : array) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
    Integer z = array[0];
    // :: error: [assignment]
    @Interned Integer y = z;
  }

  void boxArrayToObject(int[] array) {
    for (Object x : array) {
      // :: error: [assignment]
      @Interned Object y = x;
    }
  }

  void primitiveArray(int[] array) {
    for (int x : array) {
      @Interned int y = x;
    }
    for (long x : array) {
      @Interned long y = x;
    }
  }
}
