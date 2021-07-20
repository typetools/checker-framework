package org.checkerframework.checker.signedness.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The expression is {@code @}{@link SignedPositive}, and its value came from widening a value that
 * is allowed to be interpreted as unsigned.
 *
 * <p>Programmers should rarely write this annotation.
 *
 * @see SignednessGlb
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({SignednessGlb.class})
public @interface SignedPositiveFromUnsigned {}
