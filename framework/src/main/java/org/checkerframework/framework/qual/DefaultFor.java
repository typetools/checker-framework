package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation applied to the declaration of a type qualifier specifies that the given
 * annotation should be the default for
 *
 * <ul>
 *   <li>a particular location.
 *   <li>a use of a particular type.
 *   <li>a use of a particular kind of type.
 * </ul>
 *
 * @see TypeUseLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInHierarchy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultFor {
    /** @return the locations to which the annotation should be applied */
    TypeUseLocation[] value() default {};

    /** @return {@link TypeKind}s of types for which an annotation should be implicitly added */
    TypeKind[] typeKinds() default {};

    /**
     * @return {@link Class}es for which an annotation should be applied. For example, if
     *     {@code @MyAnno} is meta-annotated with {@code @DefaultFor(classes=String.class)}, then
     *     every occurrence of {@code String} is actually {@code @MyAnno String}.
     */
    Class<?>[] types() default {};
}
