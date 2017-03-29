package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

@Inherited
@Target(ElementType.TYPE)
public @interface MinLenFieldInvariant {
    /**
     * The field which has the qualifier. The field must be final and declared in a superclass of
     * the class on which the field invariant is written.
     */
    String[] field();

    /**
     * Min length of the array. Must be greater thant the min length of the array as declared in the
     * superclass.
     */
    int[] minLen();
}
