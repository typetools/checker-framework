// Test case for an array index whose type is a type variable bounded by a primitive wrapper.

public class TypeVarArrayIndex {
  <T extends Integer> int arrayIndex(T t, int[] arr) {
    // :: error: [array.access.unsafe.low] :: error: [array.access.unsafe.high]
    return arr[t];
  }

  int integerIndex(Integer i, int[] arr) {
    // :: error: [array.access.unsafe.low] :: error: [array.access.unsafe.high]
    return arr[i];
  }
}
