import java.util.ArrayList;
import java.util.List;

public class InferredTypesLockBase {
  protected final List<String> strings = new ArrayList<>();

  public static String getString() {
    return "";
  }

  public static Integer getInteger() {
    return 0;
  }
}
