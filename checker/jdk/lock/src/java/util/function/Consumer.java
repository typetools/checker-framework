package java.util.function;

public interface Consumer<T> {
    void accept(T value);
    default Consumer<T> andThen(Consumer<? super T> after) { throw new RuntimeException("skeleton method"); }
}
