package java.util;
import checkers.javari.quals.*;

public interface Iterator<E> {
    boolean hasNext() @ReadOnly;
    E next() @ReadOnly;
    void remove();
}
