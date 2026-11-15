import org.checkerframework.checker.interning.qual.Interned;

// A boxed value is not interned, even though every primitive is.
public class BoxingNotInterned {
  void box(int i) {
    // :: error: (assignment)
    @Interned Integer x = i;
    // :: error: (assignment)
    @Interned Object o = i;
    Integer y = i;
    // :: error: (assignment)
    @Interned Integer z = y;
  }
}
