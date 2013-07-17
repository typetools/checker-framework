package checkers.regex.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For char, char[], {@link Character} and subtypes of {@link CharSequence}
 * indicates a valid regular expression and holds the number of groups in
 * the regular expression.
 * <p>
 * For {@link java.util.regex.Pattern Pattern} and subtypes of
 * {@link java.util.regex.MatchResult MatchResult} indicates the number of regular
 * expression groups.
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Regex {
    /**
     * The number of groups in the regular expression.
     * Defaults to 0.
     */
    int value() default 0;
}
