import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;

public class OffsetsAndConstants {
  static int read(
      char[] a,
      @IndexOrHigh("#1") int off,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int len) {
    int sum = 0;
    for (int i = 0; i < len; i++) {
      sum += a[i + off];
    }
    return sum;
  }

  public static void main(String[] args) {
    char[] a = new char[10];

    read(a, 5, 4);

    read(a, 5, 5);

    // :: error: (argument)
    read(a, 5, 6);

    // :: error: (argument)
    read(a, 5, 7);
  }
}
