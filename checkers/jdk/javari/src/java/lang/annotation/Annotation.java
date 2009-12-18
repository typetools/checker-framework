package java.lang.annotation;
import checkers.javari.quals.*;

public @ReadOnly interface Annotation {

    boolean equals(@ReadOnly Object obj);
    int hashCode();
    String toString();
    Class<? extends Annotation> annotationType();
}
