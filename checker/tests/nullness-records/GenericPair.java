import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java16-jdk-skip-test
public record GenericPair<K, V>(K key, V value) {

  public static void foo() {
    GenericPair<String, @Nullable Integer> p = new GenericPair<>("k", null);
    // :: error: (argument)
    p.value().toString();
  }
}
