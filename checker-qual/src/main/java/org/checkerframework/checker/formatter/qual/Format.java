package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation, attached to a String type, indicates that the String may be passed to {@link
 * java.util.Formatter#format(String, Object...) Formatter.format} and similar methods.
 *
 * <p>The annotation's value represents the valid arguments that may be passed to the format method.
 * For example:
 *
 * <blockquote>
 *
 * <pre>
 * {@literal @}Format({GENERAL, INT}) String f = "String '%s' has length %d";
 *
 *  String.format(f, "Example", 7);
 * </pre>
 *
 * </blockquote>
 *
 * The annotation indicates that the format string requires any Object as the first parameter
 * ({@link ConversionCategory#GENERAL}) and an integer as the second parameter ({@link
 * ConversionCategory#INT}). The format string accepts any values as additional parameters (because
 * it ignores them).
 *
 * @see ConversionCategory
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownFormat.class)
public @interface Format {
  /**
   * An array of {@link ConversionCategory}, indicating the types of legal remaining arguments when
   * a value of the annotated type is used as the first argument to {@link
   * java.util.Formatter#format(String, Object...) Formatter.format} and similar methods.
   *
   * @return types that can be used as values when a value of this type is the format string
   */
  ConversionCategory[] value();
}
