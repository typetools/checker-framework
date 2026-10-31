import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// T's instantiation ignores annotations, because its EQUAL bound comes from a use with an explicit
// annotation, and it is substituted into S's lower bound Map<T, R> while R is still unresolved.
public class IgnoredSubstitutionWithUnresolvedVariable {
  static class Box<T> {}

  static <S extends @Nullable Object> S id(S s) {
    return s;
  }

  static <T extends @Nullable Object, R extends @Nullable Object> Map<T, R> mk(
      Box<@NonNull T> b, T other, R r) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void test(Box<@NonNull U> nonNullU, @Nullable U nble) {
    id(mk(nonNullU, nble, "x"));
    Map<@Nullable U, String> m1 = id(mk(nonNullU, nble, "x"));
    Map<@Nullable U, String> m2 = mk(nonNullU, nble, "x");
    Map<@Nullable U, @Nullable String> m3 = id(mk(nonNullU, nble, null));
    Map<@Nullable U, String> m4 = id(id(mk(nonNullU, nble, "x")));
  }
}
