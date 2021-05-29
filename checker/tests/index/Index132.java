import org.checkerframework.common.value.qual.*;

public class Index132 {
  public static String @ArrayLen({3, 4}) [] esc_quantify(String @ArrayLen({1, 2}) ... vars) {
    if (vars.length == 1) {
      return new String[] {"hello", vars[0], ")"};
    } else {
      return new String[] {"hello", vars[0], vars[1], ")"};
    }
  }
}
