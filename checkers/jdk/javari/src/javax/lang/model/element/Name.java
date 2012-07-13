package javax.lang.model.element;

import checkers.javari.quals.*;

public interface Name extends CharSequence {
    boolean equals(@ReadOnly Name this, @ReadOnly Object obj);
    int hashCode(@ReadOnly Name this);
    boolean contentEquals(@ReadOnly Name this, @ReadOnly CharSequence cs);
}
