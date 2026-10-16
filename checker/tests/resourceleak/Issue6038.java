// Test case for https://github.com/typetools/checker-framework/issues/6038

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Issue6038<T> {

  static <T> List<String> toStringList(Collection<T> list) {
    return list.stream().map(Object::toString).collect(Collectors.toList());
  }

  String dump() {
    return dump(Object::toString);
  }

  String dump(Function<T, String> contentToString) {
    return "";
  }
}
