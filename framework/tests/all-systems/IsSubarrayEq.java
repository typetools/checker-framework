import java.util.*;

public class IsSubarrayEq {
  // the Interning checker correctly issues an error below, but we would like to keep this test in all-systems.
  @SuppressWarnings("Interning")
  public static boolean isSubarrayEq(Object[] a, List<?> sub) {
      return (sub.get(0) != a[0]);
  }
}
