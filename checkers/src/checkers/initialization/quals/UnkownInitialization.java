package checkers.initialization.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nullness.quals.NonNull;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * This type qualifier belongs to the freedom-before-commitment type-system for
 * tracking initialization. This type-system is not used on its own, but in
 * conjunction with some other type-system that wants to ensure safe
 * initialization. For instance, {@link NullnessChecker} uses
 * freedom-before-commitment to track initialization of {@link NonNull} fields.
 *
 * <p>
 * This type qualifier indicates that the object might not have been fully
 * initialized. An object is fully initialized when each of its fields contains
 * a value that satisfies its type qualifier. What type qualifiers are
 * considered depends on the checker; for instance, the
 * {@link NullnessChecker} considers {@link NonNull}.
 *
 * <p>
 * Therefore, reading a field of an object of type {@link UnkownInitialization}
 * might yield a value that does not correspond to the declared type qualifier
 * for that field. For instance, in the {@link NullnessChecker}, a field
 * might be {@code null} even if it has been annotated as {@link NonNull}.
 *
 * <p>
 * More precisely, an expression of type {@code @UnkownInitialization(T.class)}
 * refers to an object that has all fields of {@code T} (and any super-classes)
 * initialized (e.g., to a non-null value in the {@link NullnessChecker}).
 * Just {@code @Raw} is equivalent to {@code @UnkownInitialization
 * Object.class} .
 */
@Documented
@SubtypeOf({})
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface UnkownInitialization {
    /**
     * The type-frame down to which the expression (of this type) has been
     * initialized at least (inclusive). That is, an expression of type
     * {@code @UnkownInitialization(T.class)} has all type-frames initialized
     * starting at {@code Object} down to (and including) {@code T}.
     */
    Class<?> value() default Object.class;
}
