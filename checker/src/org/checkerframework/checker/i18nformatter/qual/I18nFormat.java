package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation, attached to a String type, indicates that the String may be passed to {@link
 * java.text.MessageFormat#format(String, Object...) MessageFormat.format}.
 *
 * <p>The annotation's value represents the valid arguments that may be passed to the format method.
 * For example:
 *
 * <pre>{@literal @}I18nFormat({I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER})
 * String f = "{0}{1, number}"; // valid
 * String f = "{0} {1} {2}"; // error, the format string is stronger (more restrictive) than the specifiers.
 * String f = "{0, number} {1, number}"; // error, the format string is stronger (NUMBER is a subtype of GENERAL).
 * </pre>
 *
 * The annotation indicates that the format string requires any object as the first parameter
 * ({@link I18nConversionCategory#GENERAL}) and a number as the second parameter ({@link
 * I18nConversionCategory#NUMBER}).
 *
 * @see I18nConversionCategory
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@SubtypeOf(I18nUnknownFormat.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface I18nFormat {
    /**
     * An array of {@link I18nConversionCategory}, indicating the types of legal remaining arguments
     * when a value of the annotated type is used as the first argument to {@link
     * java.text.MessageFormat#format(String, Object...) Message.format}.
     */
    I18nConversionCategory[] value();
}
