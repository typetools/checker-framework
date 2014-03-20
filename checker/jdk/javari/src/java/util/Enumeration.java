package java.util;
import org.checkerframework.checker.javari.qual.*;

public interface Enumeration<E> {
    boolean hasMoreElements(@ReadOnly Enumeration<E> this);
    E nextElement();
}
