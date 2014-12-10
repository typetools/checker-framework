package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This annotation, attached to a {@link java.lang.String String} type,
 * indicates that the String may be legally passed to
 * {@link java.util.Formatter#format(String, Object...) Formatter.format}, or
 * similar functions.
 *
 * The annotation's value represents the valid parameters that may be passed to
 * the format function. For example:
 *
 * <blockquote><pre>
 * {@literal @}Format({ConversionCategory.GENERAL, ConversionCategory.INT})
 * String f = "String '%s' has length %d";
 * String.format(f, "Example", 7);
 * </pre></blockquote>
 *
 * The annotation describes that the format string requires any Object as the
 * first parameter ({@link ConversionCategory#GENERAL}) and an integer as the
 * second parameter ({@link ConversionCategory#INT}). 
 *
 * @see ConversionCategory
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf(UnknownFormat.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Format {
    ConversionCategory[] value();
}
