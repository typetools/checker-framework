// test case for issue 163: https://github.com/kelloggm/checker-framework/issues/163

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyUpperBound;

public class UBPoly {
  public static void main(String[] args) {
    char[] a = new char[10];
    poly(a, 100);
  }

  public static void poly(char[] a, @NonNegative @PolyUpperBound int i) {
    // :: error: (argument.type.incompatible)
    access(a, i);
  }

  public static void access(char[] a, @NonNegative @LTLengthOf("#1") int j) {
    char c = a[j];
  }
}
