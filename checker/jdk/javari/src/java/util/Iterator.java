package java.util;
import org.checkerframework.checker.javari.qual.*;

public interface Iterator<E> {
    boolean hasNext(@ReadOnly Iterator<E> this);
    // For a justification of this annotation, see section
    // "Iterators and their abstract state" in the Checker Framework Manual.
    E next(@ReadOnly Iterator<E> this);
    void remove();
}
