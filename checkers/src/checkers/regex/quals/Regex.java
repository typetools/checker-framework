package checkers.regex.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * For char, char[], {@link Character} and subtypes of {@link CharSequence}
 * indicates a valid regular expression and holds the number of groups in
 * the regular expression.
 * <p>
 * For {@link java.util.regex.Pattern Pattern} and subtypes of
 * {@link java.util.regex.MatchResult MatchResult} indicates the number of regular
 * expression groups.
 */
@Documented
@TypeQualifier
@Inherited
@SubtypeOf(Unqualified.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Regex {
  
    /**
     * The number of groups in the regular expression.
     * Defaults to 0.
     */
    int value() default 0;
}
