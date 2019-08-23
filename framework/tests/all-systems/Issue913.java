// Test case for Issue 913
// https://github.com/typetools/checker-framework/issues/913

class Issue913 {
    void test(Ordering<Object> o) {
        // Object whose signedness is unknown is assigned to a MultiMap which is of Signed Long type
        @SuppressWarnings("signedness")
        Multimap<Long> newMap = create(o);
    }

    static <V> Multimap<V> create(Ordering<? super V> valueComparator) {
        throw new RuntimeException();
    }

    interface Multimap<V> {}

    class Ordering<T> {}
}
