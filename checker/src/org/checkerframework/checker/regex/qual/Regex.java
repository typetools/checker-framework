package org.checkerframework.checker.regex.qual;

import org.checkerframework.checker.regex.classic.qual.UnknownRegex;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a type is annotated as {@code @Regex(n)}, then the run-time value is
 * a regular expression with <em>n</em> capturing groups.
 * <p>
 * For example, if an expression's type is <em>@Regex(2) String</em>, then
 * at run time its value will be a legal regular expression with at least
 * two capturing groups. The type states that possible run-time values
 * include <code>"(a*)(b*)"</code>, <code>"a(b?)c(d?)e"</code>, and
 * <code>"(.)(.)(.)"</code>, but not <code>"hello"</code> nor <code>"(good)bye"</code>
 * nor <code>"(a*)(b*)("</code>.
 */
//@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL })
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiRegex.class)
// Needed for classic checker
@SubtypeOf(UnknownRegex.class)
public @interface Regex {
    /**
     * The number of groups in the regular expression.
     * Defaults to 0.
     */
    int value() default 0;

    /**
     * The name of the qualifier parameter to set.
     */
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;

    /**
     * Specify that this use is a wildcard with a bound.
     */
    Wildcard wildcard() default Wildcard.NONE;
}
