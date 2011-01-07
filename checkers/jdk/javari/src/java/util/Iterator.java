package java.util;
import checkers.javari.quals.*;

public interface Iterator<E> {
    boolean hasNext() @ReadOnly;
    // For a justification of this annotation, see section
    // "Iterators and their abstract state" in the Checker Framework manual.
    E next() @ReadOnly;
    void remove();
}
