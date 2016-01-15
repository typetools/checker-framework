package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.*;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation is deprecated.  {@link MonotonicNonNull} should be used
 * instead.
 * <p>
 *
 * Indicates that a field (or variable) is lazily initialized to a non-null
 * value.  Once the field becomes non-null, it never becomes null again.
 * There is no guarantee that the field ever becomes non-null, however.
 * <p>
 *
 * Lazily initialized fields have these two properties:
 * <ol>
 * <li>The field may be assigned only non-null values.</li>
 * <li>The field may be re-assigned as often as desired.</li>
 * </ol>
 * <p>
 *
 * When the field is first read within a method, the field cannot be
 * assumed to be non-null.  After a check that a {@code LazyNonNull} field
 * holds a non-null value, all subsequent accesses <em>within that
 * method</em> can be assumed to be non-null, even after arbitrary external
 * method calls that might access the field.
 * <p>
 *
 * {@code LazyNonNull} gives stronger guarantees than {@link Nullable}.
 * After a check that a {@link Nullable} field holds a non-null value, only
 * accesses until the next non-{@link SideEffectFree} method is called can be assumed
 * to be non-null.
 * <p>
 *
 * To indicate that a {@code LazyNonNull} or {@code Nullable} field is
 * non-null whenever a particular method is called, use
 * {@link RequiresNonNull}.
 * <p>
 *
 * Final fields are treated as LazyNonNull by default.
 * <p>
 *
 * This annotation is associated with the {@link NullnessChecker}.
 *
 * @see Nullable
 * @see NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE) // not applicable to ElementType.TYPE_PARAMETER
@SubtypeOf(Nullable.class)
public @interface LazyNonNull {}
