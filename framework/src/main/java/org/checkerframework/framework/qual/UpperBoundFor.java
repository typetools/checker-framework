package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation applied to the declaration of a type qualifier specifies that the given
 * annotation should be upper bound for.
 *
 * <ul>
 *   <li>a use of a particular type.
 *   <li>a use of a particular kind of type.
 * </ul>
 *
 * @checker_framework.manual #upper-bound-for-use Upper bound of qualifiers on uses of a given type
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface UpperBoundFor {
    /**
     * Returns {@link TypeKind}s of types for which an annotation should be added by default.
     *
     * @return {@link TypeKind}s of types for which an annotation should be added by default
     */
    TypeKind[] typeKinds() default {};

    /**
     * Returns {@link Class}es for which an annotation should be applied. For example, if
     * {@code @MyAnno} is meta-annotated with {@code @DefaultFor(classes=String.class)}, then every
     * occurrence of {@code String} is actually {@code @MyAnno String}.
     *
     * @return {@link Class}es for which an annotation should be applied
     */
    Class<?>[] types() default {};
}
