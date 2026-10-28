import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MethodRefUncheckedReturnType {

  static <R> R call(Function<List, R> f) {
    throw new UnsupportedOperationException();
  }

  // Overloaded, so `MethodRefUncheckedReturnType::strings` is an inexact method reference.
  static <T> List<@Nullable String> strings(List<T> l) {
    throw new UnsupportedOperationException();
  }

  static <T> List<@Nullable String> strings(List<T> l, int i) {
    throw new UnsupportedOperationException();
  }

  // Unchecked conversion from raw `List` to `List<T>` is necessary for `strings` to be applicable,
  // so the return type of its invocation type is the raw `List` rather than
  // `List<@Nullable String>`.
  @SuppressWarnings("unchecked")
  void erasedReturnType() {
    List<String> l = call(MethodRefUncheckedReturnType::strings);
  }
}
