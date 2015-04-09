package org.checkerframework.checker.nullness.qual;

import org.checkerframework.checker.nullness.qual.KeyFor;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Used internally by the type system; should never be written by a programmer.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@InvisibleQualifier
@SubtypeOf(KeyFor.class)
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
public @interface KeyForBottom {}
