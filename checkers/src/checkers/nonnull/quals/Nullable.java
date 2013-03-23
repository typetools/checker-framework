package checkers.nonnull.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.nonnull.AbstractNullnessChecker;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * {@link Nullable} is a type annotation that indicates that the value is not
 * known to be non-null (see {@link NonNull}). Only if an expression has a
 * {@link Nullable} type it can be assigned {@code null}.
 *
 * <p>
 * This annotation is associated with the {@link AbstractNullnessChecker}.
 *
 * @see NonNull
 * @see MonotonicNonNull
 * @see AbstractNullnessChecker
 */
@Documented
@SubtypeOf({})
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Nullable {
}
