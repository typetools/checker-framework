package org.checkerframework.checker.regex.classic.qual;

import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Represents the bottom of the Regex qualifier hierarchy. This is used to make
 * the null literal a subtype of all Regex annotations.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@InvisibleQualifier
@ImplicitFor(literals = {LiteralKind.NULL},
  typeNames = {java.lang.Void.class})
@SubtypeOf({Regex.class, PartialRegex.class})
@DefaultFor(value={ TypeUseLocation.LOWER_BOUND })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface RegexBottom {}
