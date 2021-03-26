import org.checkerframework.checker.fenum.qual.FenumBottom;
import org.checkerframework.checker.fenum.qual.PolyFenum;
import org.checkerframework.checker.fenum.qual.SwingCompassDirection;

public class PolyTest {
  public static boolean flag = false;

  @PolyFenum String merge(
      @PolyFenum String a,
      @PolyFenum String b,
      @SwingCompassDirection String x,
      @FenumBottom String bot) {
    // Test lub with poly and a qualifier that isn't top or bottom.
    String y = flag ? a : x;
    // :: error: (assignment.type.incompatible)
    @PolyFenum String y2 = flag ? a : x;

    // Test lub with poly and bottom.
    // Test lub with poly and bottom.
    @PolyFenum String z = flag ? a : bot;

    // Test lub with two polys
    return flag ? a : b;
  }
}
