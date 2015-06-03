package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the Format String type hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
@TypeQualifier
@SubtypeOf({Format.class,InvalidFormat.class})
@Target({}) // empty target prevents programmers from writing this in a program
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@DefaultFor(value = {DefaultLocation.LOWER_BOUNDS})
public @interface FormatBottom {
}
