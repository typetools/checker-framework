package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation applied to the declaration of a type qualifier. It specifies that the given
 * annotation should be the default for:
 *
 * <ul>
 *   <li>all uses at a particular location,
 *   <li>all uses of a particular type, and
 *   <li>all uses of a particular kind of type.
 * </ul>
 *
 * <p>The default applies to every match for any of this annotation's conditions.
 *
 * @see TypeUseLocation
 * @see DefaultQualifier
 * @see DefaultQualifierInHierarchy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultFor {
    /**
     * Returns the locations to which the annotation should be applied.
     *
     * @return the locations to which the annotation should be applied
     */
    TypeUseLocation[] value() default {};

    /**
     * Returns {@link TypeKind}s of types for which an annotation should be implicitly added.
     *
     * @return {@link TypeKind}s of types for which an annotation should be implicitly added
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

    /**
     * @return regular expressions matching variables to whose type an annotation should be applied.
     *     A variable uses this default if it matches at least one of these regular expressions, and
     *     it matches none of the exceptions in {@link #variableNamesExceptions}.
     */
    String[] variableNames() default {};

    /** @return exceptions to regular exception rules. See {@link #variableNames}. */
    String[] variableNamesExceptions() default {};
}
