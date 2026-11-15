import org.checkerframework.checker.tainting.qual.Untainted;

// Boxing keeps the qualifier of the value being boxed.
public class BoxingRefinement {

  void assignment(@Untainted int i) {
    Integer j = i;
    @Untainted Integer k = j;
  }

  void toObject(@Untainted int i) {
    Object j = i;
    @Untainted Object k = j;
  }

  void enhancedForArray(@Untainted int[] array) {
    for (Integer x : array) {
      @Untainted Integer y = x;
    }
  }

  void tainted(int i) {
    Integer j = i;
    // :: error: (assignment)
    @Untainted Integer k = j;
  }
}
