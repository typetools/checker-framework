package java.lang;
import checkers.javari.quals.*;

import java.io.Serializable;

public abstract @ReadOnly class Enum<E extends Enum<E>>
        implements Comparable<E>, Serializable {
    private final String name;

    public final String name() {
        throw new RuntimeException("skeleton method");
    }

    private final int ordinal;

    public final int ordinal() {
        throw new RuntimeException("skeleton method");
    }

    protected Enum(String name, int ordinal) {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public final boolean equals(@ReadOnly Object other) {
        throw new RuntimeException("skeleton method");
    }

    public final int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new RuntimeException("skeleton method");
    }

    public final int compareTo(E o) {
        throw new RuntimeException("skeleton method");
    }

    public final Class<E> getDeclaringClass() {
        throw new RuntimeException("skeleton method");
    }

    public static <T extends Enum<T>>  T valueOf( Class<T> enumType,
                                                 String name) {
        throw new RuntimeException("skeleton method");
    }

    protected final void finalize() { throw new RuntimeException("skeleton method");}
}
