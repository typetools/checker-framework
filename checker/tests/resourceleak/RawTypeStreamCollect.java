import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Regression test: raw type in a stream pipeline erases the collector result to Object. The checker
 * must not crash in AsSuperVisitor when it encounters asSuper(Object, List).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RawTypeStreamCollect {

  interface Factory<T> {
    String getId();
  }

  static Stream<Factory> getFactories() {
    return Stream.empty();
  }

  static void setOptions(List<String> options) {}

  static void test() {
    // :: error: (argument) :: error: (type.arguments.not.inferred)
    setOptions(getFactories().map(Factory::getId).collect(Collectors.toList()));
  }
}
