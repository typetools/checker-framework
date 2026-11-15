import java.util.List;
import org.checkerframework.checker.tainting.qual.Untainted;

// Unboxing keeps the qualifier of the value being unboxed.
public class UnboxingRefinement {

  void assignment(@Untainted Integer i) {
    int j = i;
    @Untainted int k = j;
  }

  void conditional(@Untainted Integer i, @Untainted Integer i2, boolean b) {
    int j = b ? i : i2;
    @Untainted int k = j;
  }

  void arithmetic(@Untainted Integer i, @Untainted Integer i2) {
    int j = i + i2;
    @Untainted int k = j;
  }

  <T extends Integer> void typeVariable(@Untainted T t) {
    int j = t;
    @Untainted int k = j;
  }

  void enhancedForIterable(List<@Untainted Integer> list) {
    for (int x : list) {
      @Untainted int y = x;
    }
  }

  void enhancedForArray(@Untainted Integer[] array) {
    for (int x : array) {
      @Untainted int y = x;
    }
  }

  void tainted(Integer i) {
    int j = i;
    // :: error: (assignment)
    @Untainted int k = j;
  }
}
