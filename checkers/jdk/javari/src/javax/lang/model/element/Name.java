package javax.lang.model.element;

import checkers.javari.quals.*;

public interface Name extends CharSequence {
    boolean equals(@ReadOnly Object obj) @ReadOnly;
    int hashCode() @ReadOnly;
    boolean contentEquals(@ReadOnly CharSequence cs) @ReadOnly;
}
