import java.util.Arrays;
import org.checkerframework.checker.fenum.qual.FenumTop;

/*
 * This test case violates an assertion in the compiler.
 * It does not depend on the Fenum Checker, it breaks for any checker.
 */
public class IdentityArrayListTest {
  // The type of the third argument to Arrays.copyOf should be:
  // Class<? extends T @FenumTop []>
  // But the annotated JDK does not have annotations for the Fenum Checker.
  @SuppressWarnings("argument")
  public <T> T[] toArray(T[] a) {
    // Warnings only with -Alint=cast:strict.
    // TODO:: warning: (cast.unsafe)
    // :: warning: [unchecked] unchecked cast
    return (T[]) Arrays.copyOf(null, 0, a.getClass());
  }

  public <T> T[] toArray2(T[] a) {
    wc(null, 0, new java.util.LinkedList<T[]>());
    // TODO:: warning: (cast.unsafe)
    // :: warning: [unchecked] unchecked cast
    return (T[]) myCopyOf(null, 0, a.getClass());
  }

  public static <T, U> T[] myCopyOf(
      U[] original, int newLength, Class<? extends T @FenumTop []> newType) {
    return null;
  }

  public static <T, U> T[] wc(
      U[] original, int newLength, java.util.List<? extends T @FenumTop []> arr) {
    return null;
  }
}
