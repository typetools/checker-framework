package java.util.function;

public interface Predicate<T> {
    boolean test(T arg0);
    Predicate<T> and(Predicate<? super T> arg0);
    Predicate<T> negate();
    Predicate<T> or(Predicate<? super T> arg0);
    static <T> Predicate<T> isEqual(Object arg0) {
        return null;
    }
}
