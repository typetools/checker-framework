package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used internally by the type system; should never be written by a programmer.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@InvisibleQualifier
@SubtypeOf(KeyFor.class)
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
@ImplicitFor(literals = {LiteralKind.NULL},
  typeNames = {java.lang.Void.class})
public @interface KeyForBottom {}
