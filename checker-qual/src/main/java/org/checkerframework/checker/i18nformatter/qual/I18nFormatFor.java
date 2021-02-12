package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation indicates that when a string of the annotated type is passed as the first
 * argument to {@link java.text.MessageFormat#format(String, Object...)}, then the expression that
 * is an argument to the annotation can be passed as the remaining arguments, in varargs style.
 *
 * <p>The annotation is used to annotate a method to ensure that an argument is of a particular type
 * indicated by a format string.
 *
 * <p>Example:
 *
 * <pre> static void method(@I18nFormatFor("#2") String format, Object... arg2) {...}
 *
 * method("{0, number}", 2);</pre>
 *
 * This ensures that the second parameter ("#2") can be passed as the remaining arguments of {@link
 * java.text.MessageFormat#format(String, Object...)}, when the first argument is {@code "format"}.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(I18nUnknownFormat.class)
public @interface I18nFormatFor {
    /**
     * Indicates which formal parameter is the arguments to the format method. The value should be
     * {@code #} followed by the 1-based index of the formal parameter that is the arguments to the
     * format method, e.g., {@code "#2"}.
     *
     * @return {@code #} followed by the 1-based index of the formal parameter that is the arguments
     *     to the format method
     */
    String value();
}
