package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression with this type has no aliases.
 * In other words, no other expression, evaluated at the same program
 * point, would evaluate to the exact same object value.
 * <p>
 *
 * A constructor's return type should be annotated with <code>@Unique</code> if the
 * constructor does not leak references to the constructed object.
 * For example, the <code>String()</code> constructor return type
 * is annotated as <code>@Unique</code>.
 *
 * @see MaybeAliased
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@SubtypeOf({MaybeAliased.class})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface Unique {}