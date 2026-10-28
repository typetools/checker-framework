// Test case for https://github.com/typetools/checker-framework/issues/8340

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"all", "unchecked"}) // Just check for crashes.
public class Issue8340 {

  interface Factory<T> {
    String getId();
  }

  static Stream<Factory> getFactories() {
    return Stream.empty();
  }

  static void setOptions(List<String> options) {}

  public static void main(String[] args) {
    setOptions(getFactories().map(Factory::getId).collect(Collectors.toList()));
  }

  // Unchecked conversion is necessary for the nested invocation `first(raw)`, but not for `pick`.
  static <T> T first(List<T> l) {
    throw new UnsupportedOperationException();
  }

  static <T, U> Optional<T> pick(U u, T t) {
    throw new UnsupportedOperationException();
  }

  void nestedUncheckedInvocation(List raw) {
    setOptions(pick(first(raw), new ArrayList<String>()).orElseThrow());
  }

  static <T> List<T> copy(List<T> l) {
    throw new UnsupportedOperationException();
  }

  static <E extends Number> List<E> numbers(List<E> l) {
    throw new UnsupportedOperationException();
  }

  static <T> T id(T t) {
    return t;
  }

  // Unchecked conversion is necessary for `numbers` too, because the erased return type of the
  // nested invocation, raw `List`, is converted to `List<E>`.  So the type of `numbers(...)` is
  // the raw `List`.
  void enclosingUncheckedInvocation(List raw) {
    List<String> l1 = numbers(copy(raw));
    List<String> l2 = numbers(id(raw));
  }
}
