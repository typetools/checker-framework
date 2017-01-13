package java.util;
import org.checkerframework.checker.lock.qual.*;


import java.util.Comparator;
import java.util.function.Function;

// Javadoc says: "a comparator may optionally permit comparison of null
// arguments, while maintaining the requirements for an equivalence relation."
public interface Comparator<T extends Object> {
    public abstract int compare(T a1, T a2);
     public abstract boolean equals(@GuardSatisfied Comparator<T> this, @GuardSatisfied Object a1);

    public static <T extends Comparable<? super T>> Comparator<T> naturalOrder(){
        throw new RuntimeException("skeleton method");
    }

    public static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
        throw new RuntimeException("skeleton method");
    }

    public static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
        throw new RuntimeException("skeleton method");
    }

    public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {
        throw new RuntimeException("skeleton method");
    }
}
