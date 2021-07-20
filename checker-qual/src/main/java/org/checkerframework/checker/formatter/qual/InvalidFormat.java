package org.checkerframework.checker.formatter.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation, attached to a {@link java.lang.String String} type, indicates that the string is
 * not a legal format string. Passing the string to {@link java.util.Formatter#format(String,
 * Object...) Formatter.format} or similar methods will lead to the exception message indicated in
 * the annotation's value.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownFormat.class)
public @interface InvalidFormat {
    /**
     * Using a value of the annotated type as the first argument to {@link
     * java.util.Formatter#format(String, Object...) Formatter.format} or similar methods will lead
     * to this exception message.
     */
    String value();
}
