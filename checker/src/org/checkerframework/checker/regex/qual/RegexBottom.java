package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Regex qualifier hierarchy. This is used to make
 * the null literal a subtype of all Regex annotations.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@SubtypeOf({Regex.class, PartialRegex.class})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface RegexBottom {}
