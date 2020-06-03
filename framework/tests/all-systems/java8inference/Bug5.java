package inference.guava;

import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("all") // Just check for crashes.
public class Bug5<K, V> {

    boolean apply(Object key, V value, MyPredicate<? super Map.Entry<K, V>> predicate) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return predicate.apply(immutableEntry(k, value));
    }

    public static <K, V> Map.Entry<K, V> immutableEntry(K key, V value) {
        throw new RuntimeException();
    }

    public interface MyPredicate<T> extends Predicate<T> {
        boolean apply(T input);
    }
}
