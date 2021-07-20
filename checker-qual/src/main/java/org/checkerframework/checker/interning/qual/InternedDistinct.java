package org.checkerframework.checker.interning.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that no other value is {@code equals()} to the given value. Therefore, it is correct to
 * use == to test an InternedDistinct value for equality against any other value.
 *
 * <p>This is a stronger property than {@link Interned}, but a weaker property than every value of a
 * Java type being interned.
 *
 * <p>This annotation is trusted, not verified.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Interned.class)
@DefaultFor(value = {TypeUseLocation.LOWER_BOUND})
public @interface InternedDistinct {}
