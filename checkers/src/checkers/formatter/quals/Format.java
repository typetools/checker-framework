package checkers.formatter.quals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;
import checkers.quals.Unqualified;

/**
 * This annotation, attached to a {@link java.lang.String String} type,
 * indicates that the String may be legally passed to
 * {@link java.util.Formatter#format(String, Object...) Formatter.format}, or
 * similar functions.
 *
 * The annotation's value represents the valid parameters that may be passed to
 * the format function. For example:
 *
 * <blockquote>
 * <pre>
 * {@literal @}Format({ConversionCategory.GENERAL, ConversionCategory.INT})
 * String f = "String '%s' has length %d";
 * String.format(f, "Example", 7);
 * </pre>
 * </blockquote>
 *
 * The annotation describes that the format string requires any Object as the
 * first parameter ({@link ConversionCategory.GENERAL
 * ConversionCategory.GENERAL}) and an integer as the second parameter (
 * {@link ConversionCategory.INT ConversionCategory.INT}).
 *
 * @see ConversionCategory
 *
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf(Unqualified.class)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Format {
    ConversionCategory[] value();
}
