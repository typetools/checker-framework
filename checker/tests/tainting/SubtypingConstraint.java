import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.tainting.qual.Untainted;

public class SubtypingConstraint {
  void test() {
    Object[] o = new Object[0];
    // :: error: (type.arguments.not.inferred)
    asList(0, 0, "", Arrays.asList(o));
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T extends @Untainted Object> List<T> asList(T... a) {
    throw new RuntimeException("");
  }
}
