package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.qualframework.poly.qual.DefaultValue;
import org.checkerframework.qualframework.poly.qual.Wildcard;

/**
 * Untainted is the annotation to specify the untainted qualifier.
 *
 * @see Tainted
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiUntainted.class)
public @interface Untainted {
    /**
     * The name of the parameter to set.
     */
    String param() default DefaultValue.PRIMARY_TARGET;

    /**
     * Specify that this use is a wildcard with a bound.
     */
    Wildcard wildcard() default Wildcard.NONE;
}
