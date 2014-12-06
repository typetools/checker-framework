package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This annotation, attached to a {@link java.lang.String String} type,
 * indicates that the String may not be passed to
 * {@link java.util.Formatter#format(String, Object...) Formatter.format}, or
 * similar functions.
 * Passing it will lead to the exception message indicated in the annotation's
 * value.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf(UnknownFormat.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface InvalidFormat {
    String value();
}
