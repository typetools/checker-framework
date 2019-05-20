package org.checkerframework.framework.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A meta-annotation that indicates what qualifier should be given to literals. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface QualifierForLiterals {
    /**
     * @return {@link LiteralKind}s for which an annotation should be implicitly added. For example,
     *     if {@code @MyAnno} is meta-annotated with
     *     {@code @QualifierForLiterals(LiteralKind.STRING)}, then a literal {@code String} constant
     *     such as {@code "hello world"} has type {@code @MyAnno String}, but other occurrences of
     *     {@code String} in the source code are not affected. For String literals, also see the
     *     {@link #stringPatterns} annotation field.
     */
    LiteralKind[] value() default {};

    /**
     * @return regular expressions of string literals for which an annotation should be implicitly
     *     added. If multiple patterns match, then the string literal is given the greatest lower
     *     bound of all the matches.
     */
    String[] stringPatterns() default {};
}
