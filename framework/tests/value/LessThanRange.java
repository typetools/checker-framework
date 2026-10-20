import org.checkerframework.common.value.qual.*;

public class LessThanRange {
  public void test(
      @IntRange(from = 0, to = Integer.MAX_VALUE) int length,
      @IntRange(from = 0, to = Integer.MAX_VALUE) int i) {
    if (i < length) {
      @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int j = i;
    }
  }
}
