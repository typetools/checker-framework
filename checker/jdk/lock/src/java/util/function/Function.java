package java.util.function;

public interface Function<T, R> {
    R apply(T arg0);
    default <V> Function<V,R> compose(Function<? super V,? extends T> arg0) {
        return null;
    }
    default <V> Function<T,V> andThen(Function<? super R,? extends V> arg0) {
        return null;
    }
    static <T> Function<T,T> identity() {
        return null;
    }
}
