// Tests handling Math.min and Math.max methods.
// The upper bound of Math.max is issue panacekcz#20:
// https://github.com/panacekcz/checker-framework/issues/20

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

public class MinMaxIndex {
  // Both min and max preserve IndexFor
  void indexFor(char[] array, @IndexFor("#1") int i1, @IndexFor("#1") int i2) {
    char c = array[Math.max(i1, i2)];
    char d = array[Math.min(i1, i2)];
  }
  // Both min and max preserve IndexOrHigh
  void indexOrHigh(String str, @IndexOrHigh("#1") int i1, @IndexOrHigh("#1") int i2) {
    str.substring(Math.max(i1, i2));
    str.substring(Math.min(i1, i2));
  }
  // Combining IndexFor and IndexOrHigh
  void indexForOrHigh(String str, @IndexFor("#1") int i1, @IndexOrHigh("#1") int i2) {
    str.substring(Math.max(i1, i2));
    str.substring(Math.min(i1, i2));
    // :: error: (argument.type.incompatible)
    str.charAt(Math.max(i1, i2));
    str.charAt(Math.min(i1, i2));
  }
  // max does not work with different sequences, min does
  void twoSequences(String str1, String str2, @IndexFor("#1") int i1, @IndexFor("#2") int i2) {
    // :: error: (argument.type.incompatible)
    str1.charAt(Math.max(i1, i2));
    str1.charAt(Math.min(i1, i2));
  }
}
