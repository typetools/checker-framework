package javax.lang.model.element;

import org.checkerframework.checker.javari.qual.*;

public interface Name extends CharSequence {
    boolean equals(@ReadOnly Name this, @ReadOnly Object obj);
    int hashCode(@ReadOnly Name this);
    boolean contentEquals(@ReadOnly Name this, @ReadOnly CharSequence cs);
}
