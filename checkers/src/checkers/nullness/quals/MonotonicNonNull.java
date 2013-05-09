package checkers.nullness.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nullness.AbstractNullnessChecker;
import checkers.quals.MonotonicQualifier;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that a variable if of a monotonic non-null type; that is, the
 * variable might start out as {@link Nullable} and will monotonically change to
 * {@link NonNull}. It is not guaranteed that the variable will ever reach
 * {@link NonNull}, but if it does, it will stay {@link NonNull}.
 *
 * <p>
 * This annotation is associated with the {@link AbstractNullnessChecker}.
 *
 * @see MonotonicQualifier
 * @see AbstractNullnessChecker
 */
@Documented
@TypeQualifier
@SubtypeOf(Nullable.class)
@Target(ElementType.TYPE_USE)
@MonotonicQualifier(NonNull.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicNonNull {
}
