// Test for issue 65: https://github.com/kelloggm/checker-framework/issues/65

import org.checkerframework.checker.index.qual.*;

// This test ensures that the checker functions on primitive wrappers in
// addition to literal primitives. Primarily it focuses on Integer/int.

public class PrimitiveWrappers {

  void int_Integer_access_equivalent(@IndexFor("#3") Integer i, @IndexFor("#3") int j, int[] a) {
    a[i] = a[j];
  }

  void array_creation(@NonNegative Integer i, @NonNegative int j) {
    int[] a = new int[j];
    int[] b = new int[i];
  }
}
