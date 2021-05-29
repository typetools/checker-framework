package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that once the field (or variable) becomes non-null, it never becomes null again. There
 * is no guarantee that the field ever becomes non-null, but if it does, it will stay non-null.
 *
 * <p>Example use cases include lazy initialization and framework-based initialization in a
 * lifecycle method other than the constructor.
 *
 * <p>A monotonically non-null field has these two properties:
 *
 * <ol>
 *   <li>The field may be assigned only non-null values.
 *   <li>The field may be re-assigned as often as desired.
 * </ol>
 *
 * <p>When the field is first read within a method, the field cannot be assumed to be non-null.
 * After a check that a {@code @MonotonicNonNull} field holds a non-null value, all subsequent
 * accesses <em>within that method</em> can be assumed to be non-null, even after arbitrary external
 * method calls that might access the field.
 *
 * <p>{@code @MonotonicNonNull} gives stronger guarantees than {@link Nullable}. After a check that
 * a {@link Nullable} field holds a non-null value, only accesses until the next non-{@link
 * org.checkerframework.dataflow.qual.SideEffectFree} method is called can be assumed to be
 * non-null.
 *
 * <p>To indicate that a {@code @MonotonicNonNull} or {@code @Nullable} field is non-null whenever a
 * particular method is called, use {@code @}{@link RequiresNonNull}.
 *
 * <p>Final fields are treated as MonotonicNonNull by default.
 *
 * @see EnsuresNonNull
 * @see RequiresNonNull
 * @see MonotonicQualifier
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@SubtypeOf(Nullable.class)
@MonotonicQualifier(NonNull.class)
public @interface MonotonicNonNull {}
