import org.checkerframework.framework.qual.PolyAll;

public class PolyAllTypeVar2<E> {
    interface Comparable<T> {
        int compareTo(@PolyAll T o);
    }

    void method(Comparable<E> e1, E e2) {
        e1.compareTo(e2);
    }
}
