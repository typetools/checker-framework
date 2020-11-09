package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating the possible values for a String type. The annotation's arguments are
 * Java regular expressions. If an expression's type has this annotation, then at run time, the
 * expression evaluates to a string that matches at least one of the regular expressions. Matching
 * is via the <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html#matches-java.lang.String-">java.lang.String#matches</a>
 * method, which matches against the entire string (it does not look for a match against a
 * substring).
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
