package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This formal parameter annotation indicates that the method searches for the given value, using
 * reference equality ({@code ==}).
 *
 * <p>Within the method, the formal parameter should be compared with {@code ==} rather than with
 * {@code equals()}. However, any value may be passed to the method, and the Interning Checker does
 * not verify that use of {@code ==} within the method is logically correct.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface FindDistinct {}
