package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the Must Call type hierarchy. It represents a type that might have an
 * obligation to call any set (even an infinite set!) of methods, and therefore represents every
 * object. This type should rarely be written by programmers, because the Must Call type system
 * should be used to track specific MustCall obligations using {@link MustCall}; the Object
 * Construction Checker cannot verify that the property represented by this annotation is enforced.
 *
 * @checker_framework.manual #must-call-checker Must Call Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface MustCallUnknown {}
