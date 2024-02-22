package org.checkerframework.checker.mustcallonelements.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.mustcall.qual.Owning;

/**
 * Annotation indicating that ownership should be transferred to the annotated array for the
 * purposes of Must Call checking. In that sense, the annotation ports the semantics of
 * {@code @}{@link Owning} to arrays. The Must Call obligations of the annotated array mean that the
 * listed methods must be called on each element of the array. When assigning to a This is a
 * declaration annotation rather than a type annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
public @interface OwningArray {}
