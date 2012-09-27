import java.util.*;

public class IsSubarrayEq {
  public static boolean isSubarrayEq(Object[] a, List<?> sub) {
      return (sub.get(0) != a[0]);
  }
}