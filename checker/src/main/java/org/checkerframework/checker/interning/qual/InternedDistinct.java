package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Indicates that no other value is {@code equals()} to the given value. Therefore, it is correct to
 * use == to test an InternedDistinct value for equality against any other value.
 *
 * <p>This is a stronger property than {@link Interned}, but a weaker property than every value of a
 * Java type being interned.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@SubtypeOf(Interned.class)
@DefaultFor(value = {TypeUseLocation.LOWER_BOUND})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface InternedDistinct {}
