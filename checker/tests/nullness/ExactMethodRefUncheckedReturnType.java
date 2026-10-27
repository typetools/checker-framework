import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ExactMethodRefUncheckedReturnType {

  static <R> R call(Function<List, R> f) {
    throw new UnsupportedOperationException();
  }

  // Not overloaded and not generic, so `ExactMethodRefUncheckedReturnType::strings` is an exact
  // method reference.
  static List<@Nullable String> strings(List<String> l) {
    throw new UnsupportedOperationException();
  }

  // Unchecked conversion from raw `List` to `List<String>` is necessary for `strings` to be
  // applicable, so the return type of its invocation type is the raw `List`, and so is R.  The
  // return type of `call` is not erased.
  @SuppressWarnings("unchecked")
  void erasedReturnType() {
    List<String> l = call(ExactMethodRefUncheckedReturnType::strings);
    int size = call(ExactMethodRefUncheckedReturnType::strings).size();
  }

  static <T> List<T> withParameterizedFormal(List<String> l, T t) {
    throw new UnsupportedOperationException();
  }

  static <U> U id(U u) {
    return u;
  }

  // Unchecked conversion is necessary for `withParameterizedFormal` to be applicable, even though
  // its formal parameter type does not mention T.  So its return type is the raw `List`, and the
  // target type does not constrain T to be non-null.
  @SuppressWarnings("unchecked")
  void uncheckedForProperFormal(List raw, @Nullable String ns) {
    List<String> l = withParameterizedFormal(raw, ns);
    List<String> l2 = id(withParameterizedFormal(raw, ns));
  }
}
