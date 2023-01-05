package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * {@link Nullable} is a type annotation that makes no commitments about whether the value is {@code
 * null}. Equivalently, the type includes the {@code null} value.
 *
 * <p>The Nullness Checker issues an error if {@code null} is assigned an expression of {@link
 * NonNull} type.
 *
 * <p>Programmers typically write {@code @Nullable} to indicate that the value is not known to be
 * {@link NonNull}. However, since {@code @Nullable} is a supertype of {@code @NonNull}, an
 * expression that never evaluates to {@code null} can have a declared type of {@code @Nullable}.
 *
 * @see NonNull
 * @see MonotonicNonNull
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@QualifierForLiterals(LiteralKind.NULL)
@DefaultFor(types = Void.class)
public @interface Nullable {}
