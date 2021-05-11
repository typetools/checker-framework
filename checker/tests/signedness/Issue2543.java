import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class Issue2543 {

  public static @PolySigned int rotateRightPart1(@PolySigned int i, int distance) {
    // :: error: (shift.unsigned)
    return i >>> distance;
  }

  public static @PolySigned int rotateRightPart2(@PolySigned int i, int distance) {
    return i << -distance;
  }

  public static @PolySigned int rotateRight(@PolySigned int i, int distance) {
    // :: error: (shift.unsigned)
    return (i >>> distance) | (i << -distance);
  }

  public static @Signed int rotateRightSignedPart1(@Signed int i, int distance) {
    // :: error: (shift.unsigned)
    return i >>> distance;
  }

  public static @Signed int rotateRightSignedPart2(@Signed int i, int distance) {
    return i << -distance;
  }

  public static @Signed int rotateRightSigned(@Signed int i, int distance) {
    // :: error: (shift.unsigned)
    return (i >>> distance) | (i << -distance);
  }

  public static @Unsigned int rotateRightUnsigned(@Unsigned int i, int distance) {
    return (i >>> distance) | (i << -distance);
  }

  public static @Unsigned int rotateRightUnknownSignedness(@UnknownSignedness int i, int distance) {
    // :: error: (return)
    return (i >>> distance) | (i << -distance);
  }
}
