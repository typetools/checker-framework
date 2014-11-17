package org.checkerframework.checker.experimental.regex_qual_poly.qual;

import com.sun.source.tree.Tree;
import org.checkerframework.checker.experimental.tainting_qual_poly.qual.Tainted;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regex is the annotation to specify the regex qualifier.
 *
 * @see Tainted
 */
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL })
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiRegex.class)
public @interface Regex {
    /**
     * The number of groups in the regular expression.
     * Defaults to 0.
     */
    int value() default 0;
    // The name of the parameter to set in the annotated reference.
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;
    // Specify a wildcard with a bound.
    Wildcard wildcard() default Wildcard.NONE;
}
