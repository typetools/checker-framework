package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The expression is {@code @}{@link SignedPositive}, and its value came from widening an unsigned
 * value.
 *
 * <p>Programmers should rarely write {@code @SignedPositiveFromUnsigned}.
 *
 * @see SignednessGlb
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({SignednessGlb.class})
public @interface SignedPositiveFromUnsigned {}
