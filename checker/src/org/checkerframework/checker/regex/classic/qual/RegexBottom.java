package org.checkerframework.checker.regex.classic.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Regex qualifier hierarchy. This is used to make
 * the null literal a subtype of all Regex annotations.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@InvisibleQualifier
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@SubtypeOf({Regex.class, PartialRegex.class})
@DefaultFor(value={DefaultLocation.LOWER_BOUNDS})
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
public @interface RegexBottom {}
