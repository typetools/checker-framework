// An array element is reached through the array, so assigning to `a[i]` is covered by an
// annotation that lists `a`.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ArraySeonly {

  int[] arr = new int[10];
  static int[] staticArr = new int[10];

  @SideEffectsOnly("#1")
  void assignsElementOfParameter(int[] a) {
    a[0] = 1;
    a[0]++;
    a[0] += 2;
  }

  @SideEffectsOnly("this")
  void assignsElementOfField() {
    arr[0] = 1;
    this.arr[0] = 1;
  }

  @SideEffectsOnly("#1")
  void assignsElementOfUnlistedArray(int[] listed, int[] notListed) {
    // :: error: (purity.incorrect.sideeffectsonly)
    notListed[0] = 1;
    // :: error: (purity.incorrect.sideeffectsonly)
    staticArr[0] = 1;
  }

  // The index is not reached through the array, so listing an array does not permit modifying the
  // index.
  @SideEffectsOnly("#1")
  void assignsIndex(int[] a, int[] indices) {
    a[indices[0]] = 1;
    // :: error: (purity.incorrect.sideeffectsonly)
    indices[0] = 1;
  }
}
