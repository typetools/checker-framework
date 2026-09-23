// Test case for https://github.com/typetools/checker-framework/issues/8261
// The Checker Framework used to crash on these; this test pins down the type arguments that are
// inferred for them, which `framework/tests/all-systems/Issue8261.java` cannot check.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8261 {
  static class Strategy<W> {}

  static class FactoryClass<K> {
    FactoryClass(K k) {}
  }

  static class Buffer<K, W> {
    Buffer(Strategy<W> strategy, FactoryClass<K> factory, K key) {}
  }

  // The raw `Strategy` makes the constructor applicable only by unchecked conversion, so the type
  // of the whole expression is the raw type `Buffer`.  The constructor's parameter types are not
  // erased, though, so `K` is still inferred -- here as `@Nullable String`, from both arguments.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static void agree(Strategy raw, @Nullable String nble) {
    new Buffer<>(raw, new FactoryClass<>(nble), nble);
  }

  // The nested diamond's own type argument is inferred together with `K`, so it widens to
  // `@Nullable String` rather than being fixed at `@NonNull String` by its argument.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static void nestedDiamondWidens(Strategy raw, @Nullable String nble, @NonNull String nn) {
    new Buffer<>(raw, new FactoryClass<>(nn), nble);
  }

  // Writing the nested type argument explicitly fixes `K` to `@NonNull String`, so the `@Nullable`
  // key no longer has a consistent instantiation.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static void nestedExplicitTypeArgument(Strategy raw, @Nullable String nble, @NonNull String nn) {
    // :: error: (type.arguments.not.inferred)
    new Buffer<>(raw, new FactoryClass<@NonNull String>(nn), nble);
  }

  // Same, but with a key that agrees with the explicit nested type argument.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static void nestedExplicitTypeArgumentOk(Strategy raw, @NonNull String nn) {
    new Buffer<>(raw, new FactoryClass<@NonNull String>(nn), nn);
  }
}
