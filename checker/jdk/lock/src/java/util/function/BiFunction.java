package java.util.function;

public interface BiFunction<T, U, R> {
    R apply(T arg0, U arg1);
    default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) { throw new RuntimeException("skeleton method"); }
}
