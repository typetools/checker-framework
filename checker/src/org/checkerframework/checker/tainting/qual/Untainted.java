package org.checkerframework.checker.tainting.qual;

import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static com.sun.source.tree.Tree.Kind.STRING_LITERAL;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

/**
 * Denotes a reference that is untainted, i.e. can be trusted.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@TypeQualifier
@SubtypeOf(Tainted.class)
@ImplicitFor(trees = { STRING_LITERAL, NULL_LITERAL })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor(DefaultLocation.LOWER_BOUNDS)
public @interface Untainted {}
