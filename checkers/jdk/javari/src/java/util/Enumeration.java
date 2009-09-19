package java.util;
import checkers.javari.quals.*;

public interface Enumeration<E> {
    boolean hasMoreElements() @ReadOnly;
    E nextElement();
}
