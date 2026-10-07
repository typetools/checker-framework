// Test case for https://github.com/typetools/checker-framework/issues/6741

import java.util.ArrayList;
import java.util.List;

public class Issue6741 {

  public static <T> List<T> getInParameter(T value, Class<T> type) {
    return new ArrayList<>(5);
  }

  List<?> getInParameter() {
    var value = 5;
    var type = Integer.class;
    var result = getInParameter(value, type);
    return result;
  }
}
