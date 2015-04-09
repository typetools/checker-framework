package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * An expression with this type has no aliases.
 * In other words, no other expression, evaluated at the same program
 * point, would evaluate to the exact same object value.
 * <p>
 *
 * A constructor's return type should be annotated with <tt>@Unique</tt> if the
 * constructor does not leak references to the constructed object.
 * For example, the <tt>String()</tt> constructor return type
 * is annotated as <tt>@Unique</tt>.
 *
 * @see MaybeAliased
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@TypeQualifier
@SubtypeOf({MaybeAliased.class})
@DefaultFor(DefaultLocation.LOWER_BOUNDS)
public @interface Unique {}