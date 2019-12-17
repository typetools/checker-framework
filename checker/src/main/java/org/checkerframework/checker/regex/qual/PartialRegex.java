package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates a String that is not a syntactically valid regular expression. The String itself can be
 * stored as a parameter to the annotation, allowing the Regex Checker to verify some concatenations
 * of partial regular expression Strings.
 *
 * <p>This annotation may not be written in source code; it is an implementation detail of the Regex
 * Checker.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // empty target prevents programmers from writing this in a program
@InvisibleQualifier
@SubtypeOf(org.checkerframework.checker.regex.qual.UnknownRegex.class)
public @interface PartialRegex {

    /**
     * The String qualified by this annotation. Used to verify concatenation of partial regular
     * expressions. Defaults to the empty String.
     */
    String value() default "";
}
