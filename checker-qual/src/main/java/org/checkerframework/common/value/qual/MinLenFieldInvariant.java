package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.FieldInvariant;

/**
 * A specialization of {@link FieldInvariant} for specifying the minimum length of an array. A class
 * can be annotated with both this annotation and a {@link FieldInvariant} annotation.
 *
 * @checker_framework.manual #field-invariants Field invariants
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface MinLenFieldInvariant {

    /**
     * Min length of the array. Must be greater than the min length of the array as declared in the
     * superclass.
     */
    int[] minLen();

    /**
     * The field that has an array length qualifier in the class on which the field invariant is
     * written. The field must be final and declared in a superclass.
     */
    String[] field();
}
