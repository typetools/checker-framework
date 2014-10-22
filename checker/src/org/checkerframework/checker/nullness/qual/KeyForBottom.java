package org.checkerframework.checker.nullness.qual;

import org.checkerframework.checker.nullness.qual.KeyFor;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * TODO: document that this is the bottom type for the KeyFor system.
 *
 * @checker_framework_manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@InvisibleQualifier
@SubtypeOf(KeyFor.class)
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
public @interface KeyForBottom {}
