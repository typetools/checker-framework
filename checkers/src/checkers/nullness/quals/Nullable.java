package checkers.nullness.quals;

import checkers.nullness.AbstractNullnessChecker;
import checkers.quals.DefaultFor;
import checkers.quals.DefaultLocation;
import checkers.quals.ImplicitFor;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * @checker.framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifier
@SubtypeOf({})
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@DefaultFor({ DefaultLocation.LOCALS, DefaultLocation.IMPLICIT_UPPER_BOUNDS })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Nullable {
}
