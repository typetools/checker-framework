package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that means both {@link SideEffectFree} and {@link
 * Deterministic}. The more important of these, when performing pluggable type-checking, is usually
 * {@link SideEffectFree}.
 *
 * <p>For a discussion of the meaning of {@code Pure} on a constructor, see the documentation of
 * {@link Deterministic}.
 *
 * <p>This annotation is inherited by subtypes, just as if it were meta-annotated with
 * {@code @InheritedAnnotation}.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Pure {
    /** The type of purity. */
    enum Kind {
        /** The method has no visible side effects. */
        SIDE_EFFECT_FREE,

        /** The method returns exactly the same value when called in the same environment. */
        DETERMINISTIC
    }
}
