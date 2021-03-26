import org.checkerframework.checker.nullness.qual.*;

public class FinalVar3 {

  static Object method1(@Nullable Object arg) {
    final Object tmp = arg;
    if (tmp == null) {
      return "hello";
    }
    // The type of the final variable is correctly refined.
    tmp.hashCode();
    return "bye";
  }
}
