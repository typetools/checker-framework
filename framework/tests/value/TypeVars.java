public class TypeVars<K, V> {
    private void test(K key, V value) {
        String s = "Negative size: " + key + "=" + value;
    }
    class MyClass<T> {
        public T myMethod() {
            return null;
        }
    }
}