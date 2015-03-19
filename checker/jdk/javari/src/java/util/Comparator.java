package java.util;
import org.checkerframework.checker.javari.qual.*;

public interface Comparator<T> {
    int compare(@ReadOnly Comparator<T> this, T o1, T o2);
    boolean equals(@ReadOnly Comparator<T> this, Object obj) ;
}
