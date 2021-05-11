import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.value.qual.StringVal;

public class MultiError {
  // Testing that errors from multiple checkers are issued
  // on the same compilation unit
  // :: error: (unique.location.forbidden)
  @Unique String[] array;
  // :: error: (assignment)
  @StringVal("hello") String s = "goodbye";
}
