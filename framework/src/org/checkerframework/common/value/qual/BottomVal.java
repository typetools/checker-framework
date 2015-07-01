package org.checkerframework.common.value.qual;

import com.sun.source.tree.Tree;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents the bottom of the Constant Value qualifier hierarchy.  It means that
 * the value always has the value null or that the expression is dead code.
 * <p>
 *
 * This annotation may not be written in source code; it is an implementation
 * detail of the Constant Value Checker.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ ArrayLen.class, BoolVal.class, DoubleVal.class,
        IntVal.class, StringVal.class })
@Target({})
// empty target prevents programmers from writing this in a program
public @interface BottomVal {
}
