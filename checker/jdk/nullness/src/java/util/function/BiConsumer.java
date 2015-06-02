package java.util.function;

public interface BiConsumer<T, U> {
    void accept(T arg0, U arg1);
    BiConsumer<T,U> andThen(BiConsumer<? super T,? super U> arg0);
}
