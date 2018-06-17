package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * An expression with this type might have an alias. In other words, some other expression,
 * evaluated at the same program point, might evaluate to the exact same object value.
 *
 * @see Unique
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */
@Documented
@DefaultQualifierInHierarchy
@DefaultFor({TypeUseLocation.UPPER_BOUND, TypeUseLocation.LOWER_BOUND})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@ImplicitFor(literals = LiteralKind.NULL, typeNames = java.lang.Void.class)
@SubtypeOf({})
public @interface MaybeAliased {}
