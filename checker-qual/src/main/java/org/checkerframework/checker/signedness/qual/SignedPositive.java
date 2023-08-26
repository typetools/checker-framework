package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The expression's value is in the signed positive range; that is, its most significant bit is
 * zero. The value has the same interpretation as {@code @}{@link Signed} and {@code @}{@link
 * Unsigned} &mdash; both interpretations are equivalent.
 *
 * <p>Programmers should rarely write {@code @SignedPositive}. Instead, the programmer should write
 * {@code @}{@link Signed} or {@code @}{@link Unsigned} to indicate how the programmer intends the
 * value to be interpreted.
 *
 * <p>{@code @SignedPositive} corresponds to {@code @}{@link
 * org.checkerframework.checker.index.qual.NonNegative NonNegative} in the Index Checker's type
 * system.
 *
 * @see SignednessGlb
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SignednessGlb.class)
public @interface SignedPositive {}
