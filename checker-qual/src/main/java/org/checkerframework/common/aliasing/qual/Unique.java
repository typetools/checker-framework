package org.checkerframework.common.aliasing.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An expression with this type has no aliases. In other words, no other expression, evaluated at
 * the same program point, would evaluate to the exact same object value.
 *
 * <p>A constructor's return type should be annotated with {@code @Unique} if the constructor does
 * not leak references to the constructed object. For example, the {@code String()} constructor
 * return type is annotated as {@code @Unique}.
 *
 * @see MaybeAliased
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({MaybeAliased.class})
public @interface Unique {}
