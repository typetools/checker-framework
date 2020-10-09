package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating the possible values for a String type. If an expression's type has this
 * annotation, then at run time, the expression evaluates to a string that matches at least one of
 * the annotation's arguments, which are Java regular expressions.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf({UnknownVal.class})
public @interface MatchesRegex {
    /**
     * A set of Java regular expressions.
     *
     * @return the regular expressions
     */
    String[] value();
}
