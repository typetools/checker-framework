// Test case for Issue 2199.
@SuppressWarnings("unchecked")
class Issue2199 {
    static class StrangeConstructorTypeArgs<K, V> {
        public StrangeConstructorTypeArgs(Abstract<String, byte[]> abs) {}
    }

    abstract static class Abstract<KEY, VALUE> {}

    static class Concrete<K, V> extends Abstract<K, V> {}

    static StrangeConstructorTypeArgs getStrangeConstructorTypeArgs() {
        return new StrangeConstructorTypeArgs(new Concrete<>());
    }
}
