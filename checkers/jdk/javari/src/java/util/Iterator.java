package java.util;
import checkers.javari.quals.*;

public interface Iterator<E> {
    boolean hasNext(@ReadOnly Iterator this);
    // For a justification of this annotation, see section
    // "Iterators and their abstract state" in the Checker Framework manual.
    E next(@ReadOnly Iterator this);
    void remove();
}
