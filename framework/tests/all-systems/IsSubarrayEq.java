import java.util.List;
import org.checkerframework.common.value.qual.MinLen;

@SuppressWarnings("ainfertest") // only check WPI for crashes
public class IsSubarrayEq {
  // The Interning checker correctly issues an error below, but we would like to keep this test in
  // all-systems.
  // Fenum Checker should not issue a warning.
  @SuppressWarnings({"interning", "fenum:return"})
  public static boolean isSubarrayEq(Object @MinLen(1) [] a, List<?> sub) {
    return (sub.get(0) != a[0]);
  }
}
