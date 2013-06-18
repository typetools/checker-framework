package checkers.formatter.quals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * This annotation, attached to a {@link java.lang.String String} type,
 * indicates that the String may not be passed to
 * {@link java.util.Formatter#format(String, Object...) Formatter.format}, or
 * similar functions.
 * Passing it will lead to the exception message indicated in the annotation's
 * value.
 *
 * This annotation is used internally only.
 *
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface InvalidFormat {
    String value();
}
