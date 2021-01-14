import java.util.List;
import java.util.function.Function;

abstract class Issue4115 {

    interface V {}

    abstract <F, T> Iterable<T> transform(Iterable<F> i, Function<? super F, ? extends T> f);

    abstract <E> List<E> copyOf(Iterable<? extends E> e);

    List<V> generateAppSuggestions(List<Integer> xs) {
        return copyOf(transform(xs, x -> new V() {}));
    }
}
