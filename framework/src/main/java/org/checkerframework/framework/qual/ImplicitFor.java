package org.checkerframework.framework.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that specifies the trees and types for which the framework should automatically
 * add that qualifier. These types and trees can be specified via a combination of attributes.
 *
 * <p>For example, the {@code Nullable} annotation is annotated with
 *
 * <pre>
 *   &#064;ImplicitFor(literals = LiteralKind.NULL)
 * </pre>
 *
 * to denote that the literal {@code null} always has the type qualifier {@code @Nullable}.
 *
 * @checker_framework.manual #creating-declarative-type-introduction Declaratively specifying
 *     implicit annotations
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ImplicitFor {

    /**
     * @return {@link LiteralKind}s for which an annotation should be implicitly added. For example,
     *     if {@code @MyAnno} is meta-annotated with
     *     {@code @ImplicitFor(literals={LiteralKind.STRING})}, then a literal {@code String}
     *     constant such as {@code "hello world"} has type {@code @MyAnno String}, but other
     *     occurrences of {@code String} in the source code are not affected. For String literals,
     *     also see the {@link #stringPatterns} annotation field.
     */
    LiteralKind[] literals() default {};

    /** @return {@link TypeKind}s of types for which an annotation should be implicitly added */
    TypeKind[] types() default {};

    /**
     * @return {@link Class}es (in the actual program) for which an annotation should be implicitly
     *     added. For example, if {@code @MyAnno} is meta-annotated with
     *     {@code @ImplicitFor(typeNames=String.class)}, then every occurrence of {@code String} is
     *     actually {@code @MyAnno String}. This has the same effect as writing the annotation on
     *     the class definition (possibly in an annotated library):
     *     <pre>
     *   class @MyAnno String {...}
     * </pre>
     *     As another example, {code java.lang.Void.class} should receive the same annotation as the
     *     {@code null} literal.
     */
    Class<?>[] typeNames() default {};

    /**
     * @return regular expressions of string literals for which an annotation should be implicitly
     *     added. If multiple patterns match, then the string literal is given the greatest lower
     *     bound of all the matches.
     */
    String[] stringPatterns() default {};

    // TODO: do we need an option to provide implicits for locations
    // specified by a TypeUseLocation (which should then be renamed)?
}
