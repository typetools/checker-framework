// Test case for kelloggm 218
// https://github.com/kelloggm/checker-framework/issues/218

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class Modulo {
  void m1(Object[] a, @IndexOrHigh("#1") int i, @NonNegative int j) {
    @IndexFor("a") int k = j % i;
  }

  void m1p(Object[] a, @Positive @LTEqLengthOf("#1") int i, @Positive int j) {
    @IndexFor("a") int k = j % i;
  }

  void m2(Object[] a, int i, @IndexFor("#1") int j) {
    @IndexFor("a") int k = j % i;
  }

  void m2(Object[] a, Object[] b, @IndexFor("#1") int i, @IndexFor("#2") int j) {
    @IndexFor({"a", "b"}) int k = j % i;
  }
}
