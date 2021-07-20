package org.checkerframework.checker.initialization.qual;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This type qualifier indicates that an object is (definitely) in the process of being
 * constructed/initialized. The type qualifier also indicates how much of the object has already
 * been initialized. Please see the manual for examples of how to use the annotation (the link
 * appears below).
 *
 * <p>Consider a class {@code B} that is a subtype of {@code A}. At the beginning of the constructor
 * of {@code B}, {@code this} has the type {@code @UnderInitialization(A.class)}, since all fields
 * of {@code A} have been initialized by the super-constructor. Inside the constructor body, as soon
 * as all fields of {@code B} are initialized, then the type of {@code this} changes to
 * {@code @UnderInitialization(B.class)}.
 *
 * <p>Code is allowed to store potentially not-fully-initialized objects in the fields of a
 * partially-initialized object, as long as all initialization is complete by the end of the
 * constructor.
 *
 * <p>What type qualifiers on the field are considered depends on the checker; for instance, the
 * {@link org.checkerframework.checker.nullness.NullnessChecker} considers {@link NonNull}. The
 * initialization type system is not used on its own, but in conjunction with some other type-system
 * that wants to ensure safe initialization.
 *
 * <p>When an expression has type {@code @UnderInitialization}, then no aliases that are typed
 * differently may exist.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @checker_framework.manual #underinitialization-examples Examples of the @UnderInitialization
 *     annotation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownInitialization.class)
public @interface UnderInitialization {
    /**
     * The type-frame down to which the expression (of this type) has been initialized at least
     * (inclusive). That is, an expression of type {@code @UnderInitialization(T.class)} has all
     * type-frames initialized starting at {@code Object} down to (and including) {@code T}.
     *
     * @return the type whose fields are fully initialized
     */
    Class<?> value() default Object.class;
}
