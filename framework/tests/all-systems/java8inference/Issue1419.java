// Test case for Issue 1419.
// https://github.com/typetools/checker-framework/issues/1419

abstract class Issue1419 {
    class Map<A> {}

    class EnumMap<C extends Enum<C>> extends Map<C> {}

    abstract <E extends Enum<E>> Map<E> foo(Map<E> map);

    @SuppressWarnings("unchecked")
    <G> Map<G> bar(Map<? extends G> map) {
        return foo((EnumMap) map);
    }
}
