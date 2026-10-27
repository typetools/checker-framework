import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

// Unchecked conversion erases the return type of only the invocation whose own arguments needed it
// (JLS 15.12.2.6), as javac does.
public class UncheckedConversionAttribution {

  static <T> T nested(Function<String, List<T>> f) {
    throw new UnsupportedOperationException();
  }

  static <U> List<@Nullable String> outer(U u) {
    throw new UnsupportedOperationException();
  }

  static <T> List<@Nullable String> outerWithLambda(T t, Function<String, List<T>> f) {
    throw new UnsupportedOperationException();
  }

  // Unchecked conversion is necessary to check the lambda, which is an argument of `nested` rather
  // than of `outer`.  So the return type of `outer` is not erased.
  @SuppressWarnings("unchecked")
  void nestedLambda(List raw) {
    // :: error: (assignment)
    List<String> l = outer(nested(s -> raw));
  }

  // Unchecked conversion is necessary to check the lambda, which is an argument of
  // `outerWithLambda`, even though the lambda is not pertinent to applicability.  So the return
  // type of `outerWithLambda` is the raw `List`.
  @SuppressWarnings("unchecked")
  void ownLambda(List raw) {
    List<String> l = outerWithLambda(1, s -> raw);
  }
}
