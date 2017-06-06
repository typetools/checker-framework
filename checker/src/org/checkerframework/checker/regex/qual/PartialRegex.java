package org.checkerframework.checker.regex.qual;

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
@InvisibleQualifier
@SubtypeOf(org.checkerframework.checker.regex.qual.UnknownRegex.class)
@Target({}) // empty target prevents programmers from writing this in a program
public @interface PartialRegex {

    /**
     * The String qualified by this annotation. Used to verify concatenation of partial regular
     * expressions. Defaults to the empty String.
     */
    String value() default "";
}
