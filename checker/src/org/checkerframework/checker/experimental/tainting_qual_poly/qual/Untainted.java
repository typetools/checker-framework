package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.*;

/**
 * Untainted is the annotation to specify the untainted qualifier.
 *
 * @see Tainted
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiUntainted.class)
public @interface Untainted {
    // The name of the parameter to set in the annotated reference.
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // Specify a wildcard with a bound.
    Wildcard wildcard() default Wildcard.NONE;
}

