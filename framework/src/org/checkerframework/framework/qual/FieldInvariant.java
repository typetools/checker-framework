package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In the class in which the field invariant holds, then field[i] has annotation[i].
 *
 * <p>This annotation is inherited so, if a superclass is annotation with @FieldInvariant, it's
 * subclasses have the same annotation. If subclass has it's own @FieldInvariant, then it must
 * include the fields in the superclass annotation and those fields' annotations must be a subtype
 * (or equal) to the annotations for those fields in the the superclass @FieldInvariant.
 */
@Target({ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldInvariant {
    /* The field which has the qualifier.  The field must
     * be final and declared in a superclass of the class on which the field invariant applies.*/
    String[] field();

    /**
     * The qualifier on field. Must be a subtype of the annotation on the declaration of the field.
     */
    Class<? extends Annotation>[] qualifier();
}
