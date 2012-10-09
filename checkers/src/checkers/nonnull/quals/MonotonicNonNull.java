package checkers.nonnull.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nonnull.AbstractNonNullChecker;
import checkers.quals.MonotonicAnnotation;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Indicates that a variable if of a monotonic non-null type; that is, the
 * variable might start out as {@link Nullable} and will monotonically change to
 * {@link NonNull}. It is not guaranteed that the variable will ever reach
 * {@link NonNull}, but if it does, it will stay {@link NonNull}.
 *
 * <p>
 * This annotation is associated with the {@link AbstractNonNullChecker}.
 *
 * @see MonotonicAnnotation
 * @see AbstractNonNullChecker
 */
@Documented
@TypeQualifier
@SubtypeOf(Nullable.class)
@Target(ElementType.TYPE_USE)
@MonotonicAnnotation(NonNull.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicNonNull {
}
