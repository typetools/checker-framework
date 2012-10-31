package checkers.regex.quals;

import java.lang.annotation.Target;

import checkers.quals.InvisibleQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * Indicates a String that is not a syntactically valid regular expression.
 * The String itself can be stored as a parameter to the annotation,
 * allowing the Regex Checker to verify some concatenations of partial
 * regular expression Strings.
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf(Unqualified.class)
@Target({}) // empty target prevents programmers from writing this in a program
public @interface PartialRegex {

    /**
     * The String qualified by this annotation. Used to verify concatenation
     * of partial regular expressions. Defaults to the empty String.
     */
    String value() default "";
}
