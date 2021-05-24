// Test case for kelloggm 214
// https://github.com/kelloggm/checker-framework/issues/214

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class ShiftRight {
  void indexFor(Object[] a, @IndexFor("#1") int i) {
    @IndexFor("a") int o = i >> 2;
    @IndexFor("a") int p = i >>> 2;
  }

  void indexOrHigh(Object[] a, @IndexOrHigh("#1") int i) {
    @IndexOrHigh("a") int o = i >> 2;
    @IndexOrHigh("a") int p = i >>> 2;
    // Not true if a.length == 0
    // :: error: (assignment)
    @IndexFor("a") int q = i >> 2;
  }

  void negative(Object[] a, @LTLengthOf(value = "#1", offset = "100") int i) {
    // Not true for some negative i
    // :: error: (assignment)
    @LTLengthOf(value = "#1", offset = "100") int q = i >> 2;
  }
}
