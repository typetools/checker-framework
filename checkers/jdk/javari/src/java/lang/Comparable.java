package java.lang;
import checkers.javari.quals.*;
import java.util.*;

public interface Comparable<T> {
    public int compareTo(@ReadOnly Comparable this, T o);
}
