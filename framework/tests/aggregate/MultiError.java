import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.reflection.qual.MethodVal;
import org.checkerframework.common.value.qual.StringVal;

public class MultiError {
  // Testing that errors from multiple checkers are issued
  // on the same compilation unit
  // :: error: (unique.location.forbidden)
  @Unique String[] array;
  // :: error: (assignment.type.incompatible)
  @StringVal("hello") String s = "goodbye";

  @MethodVal(
      className = "c",
      methodName = "m",
      params = {0, 0})
  // :: error: (invalid.methodval)
  Object o;
}
