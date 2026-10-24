// Test case for an array index or array length whose type is a type variable bounded by a primitive
// wrapper.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.NonNegative;

public class TypeVarArrayIndex {
  <T extends Integer> int arrayIndex(T t, int[] arr) {
    // :: error: [array.access.unsafe.low] :: error: [array.access.unsafe.high]
    return arr[t];
  }

  int integerIndex(Integer i, int[] arr) {
    // :: error: [array.access.unsafe.low] :: error: [array.access.unsafe.high]
    return arr[i];
  }

  <T extends @IndexFor("#2") Integer> int indexForIndex(T t, int[] arr) {
    return arr[t];
  }

  <T extends @NonNegative Integer> int[] nonNegativeLength(T t) {
    return new int[t];
  }

  <T extends Integer> int[] unknownLength(T t) {
    // :: error: [array.length.negative]
    return new int[t];
  }
}
