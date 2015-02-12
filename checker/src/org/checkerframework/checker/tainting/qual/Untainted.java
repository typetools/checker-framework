package org.checkerframework.checker.tainting.qual;

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
    /**
     * The name of the parameter to set.
     */
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;

    /**
     * Specify that this use is a wildcard with a bound.
     */
    Wildcard wildcard() default Wildcard.NONE;
}

