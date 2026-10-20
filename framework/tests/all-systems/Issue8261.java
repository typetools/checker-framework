// Test case for https://github.com/typetools/checker-framework/issues/8261

import java.io.Serializable;

@SuppressWarnings("all") // Just check for crashes.
public class Issue8261 {
  static class Strategy<W> {}

  static class Reduce<K> {}

  static class FactoryClass<K> {}

  static class Buffer1<K, W> {
    Buffer1(Strategy<W> strategy, FactoryClass<K> factory) {}
  }

  static <K> FactoryClass<K> factoryOf() {
    throw new UnsupportedOperationException();
  }

  // The raw `Strategy` argument makes the constructor applicable only by unchecked conversion, so
  // the type of the whole expression is the raw type `Buffer1`.  The constructor's parameter types
  // are not erased, though, so `new FactoryClass<>()` still needs type argument inference, and it
  // must agree with the type argument inferred for `Buffer1`'s `K`.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static <K> Buffer1<K, Object> diamondArgument(Strategy raw) {
    return new Buffer1<>(raw, new FactoryClass<>());
  }

  // Same, for a generic method invocation rather than a diamond.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static <K> Buffer1<K, Object> methodArgument(Strategy raw) {
    return new Buffer1<>(raw, factoryOf());
  }

  // A diamond with an anonymous class body.  This one never took the `isRawCall` shortcut, because
  // javac's type for the expression is the anonymous class's type rather than the raw `Buffer1`.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static <K> Buffer1<K, Object> anonymousClassBody(Strategy raw) {
    return new Buffer1<>(raw, new FactoryClass<>()) {};
  }

  // The constructor is invoked on a raw type, so its parameter types are erased and there is
  // nothing to infer for them.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static Buffer1 rawConstructor(Strategy raw) {
    return new Buffer1(raw, factoryOf());
  }

  interface Factory<K> {
    Object forKey(K key);
  }

  interface SerFactory<K> extends Factory<K>, Serializable {}

  static class Buffer2<K, W> {
    Buffer2(Strategy<W> strategy, Factory<K> factory, Reduce<K> reduce) {}
  }

  static <K> Reduce<K> buffering() {
    throw new UnsupportedOperationException();
  }

  // Here the unchecked conversion comes from a lambda cast to a raw type.
  @SuppressWarnings({"rawtypes", "unchecked"})
  static <K> Buffer2<K, Object> rawCastArgument(Strategy<Object> strategy) {
    return new Buffer2<>(strategy, (SerFactory) key -> new Object(), buffering());
  }
}
