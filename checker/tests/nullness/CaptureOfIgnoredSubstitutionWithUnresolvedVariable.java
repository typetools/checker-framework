import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// T's instantiation ignores annotations, because its EQUAL bound comes from a use with an explicit
// annotation.  T is substituted into Map<T, R> while R is still unresolved, so the substitution is
// deferred.  wl's result is wildcard-parameterized, so inferring a call that has it as an argument
// or as its target uses capture variables.
public class CaptureOfIgnoredSubstitutionWithUnresolvedVariable {
  static class Box<T> {}

  static <S extends @Nullable Object> S id(S s) {
    return s;
  }

  static <S extends @Nullable Object> List<? extends S> wl(S s) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object, R extends @Nullable Object> Map<T, R> mk(
      Box<@NonNull T> b, T other, R r) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void test(Box<@NonNull U> nonNullU, @Nullable U nble) {
    wl(mk(nonNullU, nble, "x"));
    // TODO: This is a false positive.
    // :: error: (argument)
    id(wl(mk(nonNullU, nble, "x")));
    // TODO: This is a false positive.
    // :: error: (type.arguments.not.inferred)
    List<? extends Map<@Nullable U, String>> l1 = wl(mk(nonNullU, nble, "x"));
    // TODO: This is a false positive.
    // :: error: (type.arguments.not.inferred)
    List<? extends Map<@Nullable U, String>> l2 = id(wl(mk(nonNullU, nble, "x")));
    // TODO: This is a false positive.
    // :: error: (argument)
    id(wl(mk(nonNullU, nble, "x"))).get(0).toString();
  }
}
