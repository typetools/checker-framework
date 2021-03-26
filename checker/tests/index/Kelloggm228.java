import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.Positive;

public class Kelloggm228 {
  public void subList(
      @IndexOrHigh("this") @LessThan("#2 + 1") int fromIndex, @IndexOrHigh("this") int toIndex) {
    if (fromIndex == toIndex) {
      return;
    }

    @Positive int x = toIndex;
  }
}
