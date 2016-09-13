// Test case for Issue 913
// https://github.com/typetools/checker-framework/issues/913
// @skip-test

class Issue913 {
    void test(Ordering<Object> o) {
        Multimap<Long> newMap = create(o);
    }

    static <V> Multimap<V> create(Ordering<? super V> valueComparator) {
        return null;
    }

    interface Multimap<V> {}

    class Ordering<T> {}
}
