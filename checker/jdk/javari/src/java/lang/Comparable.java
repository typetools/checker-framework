package java.lang;
import org.checkerframework.checker.javari.qual.*;
import java.util.*;

public interface Comparable<T> {
    public int compareTo(@ReadOnly Comparable<T> this, T o);
}
