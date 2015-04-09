package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultForInUntyped;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

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
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifier
@SubtypeOf({})
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@DefaultFor({ DefaultLocation.LOCAL_VARIABLE, DefaultLocation.RESOURCE_VARIABLE,
            DefaultLocation.IMPLICIT_UPPER_BOUNDS })
@DefaultForInUntyped({ DefaultLocation.RETURNS })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Nullable {
}
