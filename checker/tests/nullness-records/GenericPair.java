import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
public record GenericPair<K, V>(K key, V value) {

    public static void foo() {
        GenericPair<String, @Nullable Integer> p = new GenericPair<>("k", null);
        // :: error: (dereference.of.nullable)
        p.value().toString();
    }
}
