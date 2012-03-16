package java.util;
import checkers.javari.quals.*;

public interface Comparator<T> {
    int compare(T o1, T o2) @ReadOnly;
    boolean equals(Object obj) @ReadOnly ;
}
