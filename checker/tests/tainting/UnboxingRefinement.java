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

  void widening(@Untainted Integer i) {
    long j = i;
    @Untainted long k = j;
  }

  void enhancedForArray(@Untainted Integer[] array) {
    for (int x : array) {
      @Untainted int y = x;
    }
  }

  void tainted(Integer i) {
    int j = i;
    // :: error: [assignment]
    @Untainted int k = j;
  }
}
