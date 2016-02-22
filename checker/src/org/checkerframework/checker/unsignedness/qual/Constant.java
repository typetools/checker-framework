package org.checkerframework.checker.unsignedness.qual;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Constant is a type qualifier which indicates that a value
 * is a compile-time constant, and could be {@link Unsigned} or 
 * {@link Signed}.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf( { Unsigned.class, Signed.class } )
@ImplicitFor(
    trees = {
        Tree.Kind.INT_LITERAL,
        Tree.Kind.LONG_LITERAL,
        Tree.Kind.CHAR_LITERAL
    } )
public @interface Constant { }
