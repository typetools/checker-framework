import java.util.Collections;
import java.util.List;

// A method invocation whose return type is an inference variable and whose target type is a
// primitive type.  See JLS 18.5.2.1.
public class PrimitiveTarget2 {
  static <T> T id(T t) {
    return t;
  }

  static <T> T uninstantiated() {
    throw new AssertionError();
  }

  // A primitive wrapper class is a bound of the inference variable, so the inference variable is
  // resolved eagerly.
  void wrapperBound(Integer i, List<Integer> list) {
    int a = id(i);
    long b = id(i);
    double c = Collections.max(list);
  }

  // No primitive wrapper class is a bound of the inference variable.
  void noWrapperBound() {
    int a = uninstantiated();
    long b = uninstantiated();
    char c = uninstantiated();
    boolean d = uninstantiated();
  }
}
