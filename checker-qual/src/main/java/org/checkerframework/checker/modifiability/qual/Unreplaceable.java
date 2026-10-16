package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Calling replace operations such as {@code set}, {@code replaceAll}, etc. on this collection will
 * throw {@link UnsupportedOperationException}.
 *
 * <p>No guarantees are made about grow or shrink operations.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(MaybeReplaceable.class)
public @interface Unreplaceable {}
