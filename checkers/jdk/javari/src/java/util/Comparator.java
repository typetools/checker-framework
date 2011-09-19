package java.util;
import checkers.javari.quals.*;

public interface Comparator<T> {
    int compare(@ReadOnly Comparator this, T o1, T o2);
    boolean equals(@ReadOnly Comparator this, Object obj) ;
}
