package org.checkerframework.checker.experimental.regex_qual_poly.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wild is equivalent to the wildcard operator.
 *
 * @see org.checkerframework.checker.experimental.tainting_qual_poly.qual.Wild
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiWild.class)
public @interface Wild {
    // The name of the parameter to set.
    String param();
}
