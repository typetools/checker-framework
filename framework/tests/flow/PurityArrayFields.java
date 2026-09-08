import org.checkerframework.dataflow.qual.SideEffectFree;

// Tests that a constructor may write an element of an array that a field of its own class holds,
// but only if no other code can have a reference to that array.
public class PurityArrayFields {

  // The arrays are created by the class and never escape it, so the constructor owns them.
  static class ArrayField {
    int[] a = new int[3];
    int[][] b = new int[2][2];
    int[] c = {1, 2, 3};

    @SideEffectFree
    ArrayField() {
      a[0] = 1;
      b[0][1] = 2;
      c[0] = 3;
    }

    // Writing the array anywhere else is still a side effect.
    @SideEffectFree
    void set() {
      // :: error: [purity.not.sideeffectfree.assign.array]
      a[2] = 4;
    }
  }

  // A constructor may not write an element of an array that it did not create, because other code
  // may hold a reference to that array.
  static class AliasedArrayField {
    int[] a;

    @SideEffectFree
    AliasedArrayField(int[] arg) {
      a = arg;
      // :: error: [purity.not.sideeffectfree.assign.array]
      a[0] = 1;
    }
  }

  // The array must be freshly created no matter how indirectly it is stored in the field.
  static class IndirectlyAliasedArrayField {
    int[] a = new int[3];

    @SideEffectFree
    IndirectlyAliasedArrayField(int[] arg) {
      int[] local = arg;
      a = local;
      // :: error: [purity.not.sideeffectfree.assign.array]
      a[0] = 1;
    }
  }

  // A fresh array whose elements are aliases may have its own elements written, but not the
  // elements of the arrays that it holds.
  static class FreshArrayOfAliases {
    int[][] b;

    @SideEffectFree
    FreshArrayOfAliases(int[] arg) {
      b = new int[][] {arg};
      b[0] = arg;
      // :: error: [purity.not.sideeffectfree.assign.array]
      b[0][0] = 1;
    }
  }

  // An array that escapes to other code may not be written, even though it is freshly created.
  static class EscapingArrayField {
    int[] a = new int[3];

    @SideEffectFree
    EscapingArrayField() {
      // :: error: [purity.not.sideeffectfree.assign.array]
      a[0] = 1;
    }

    int[] leak() {
      return a;
    }
  }

  // A static field is not part of the object under construction, so its array is not the
  // constructor's to write.
  static class StaticArrayField {
    static int[] s = new int[3];

    @SideEffectFree
    StaticArrayField() {
      // :: error: [purity.not.sideeffectfree.assign.array]
      s[0] = 1;
    }
  }

  // The array of another object of the same class is not the constructor's to write.
  static class OtherObjectArrayField {
    int[] a = new int[3];

    @SideEffectFree
    OtherObjectArrayField(OtherObjectArrayField other) {
      a[0] = 1;
      // :: error: [purity.not.sideeffectfree.assign.array]
      other.a[0] = 2;
    }
  }
}
