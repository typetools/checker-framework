package org.checkerframework.checker.nullness.qual;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Nullable} is a type annotation that indicates that the value is not
 * known to be non-null (see {@link NonNull}). Only if an expression has a
 * {@link Nullable} type may it be assigned {@code null}.
 *
 * <p>
 * This annotation is associated with the {@link AbstractNullnessChecker}.
 *
 * @see NonNull
 * @see MonotonicNonNull
 * @see AbstractNullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SubtypeOf({})
@ImplicitFor(literals = { LiteralKind.NULL }, typeNames = { java.lang.Void.class })
@DefaultInUncheckedCodeFor({ TypeUseLocation.RETURN, TypeUseLocation.UPPER_BOUND })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Nullable {
}
