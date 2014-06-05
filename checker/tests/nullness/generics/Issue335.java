// Test case for Issue 335:
// https://code.google.com/p/checker-framework/issues/detail?id=335
//@skip-test
import org.checkerframework.checker.nullness.qual.Nullable;

class Pair<A, B> {
  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    throw new RuntimeException();
  }
}

class Optional<T> {
  public static <T> Optional<T> of(T reference) {
    throw new RuntimeException();
  }
}

class Issue335 {
  Optional<Pair<String, String>> m(String one, String two) {
    return Optional.of(Pair.of(one, two));
  }
}
