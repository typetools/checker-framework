package org.checkerframework.checker.experimental.regex_qual_poly.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Var is a qualifier parameter use.
 *
 * @see org.checkerframework.checker.experimental.tainting_qual_poly.qual.Var
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiVar.class)
public @interface Var {
    // Specify which parameter this @Var is a use for.
    String arg()  default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // The name of the parameter to set.
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // Specify a wildcard with a bound.
    Wildcard wildcard() default Wildcard.NONE;
}

