import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CopyOfArray {
  protected void makeCopy(Object[] args, int i) {
    Object[] copy1 = Arrays.copyOf(args, args.length);
    @Nullable Object[] copy3 = Arrays.copyOf(args, args.length);

    // :: (assignment)
    Object[] copy11 = Arrays.copyOf(args, i);
    @Nullable Object[] copy13 = Arrays.copyOf(args, i);
  }
}
