package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the MethodVal qualifier hierarchy. This is used to
 * make the <tt>null</tt> literal a subtype of all MethodVal annotations.
 * <p>
 * 
 * This annotation may not be written in source code; it is an implementation
 * detail of the checker.
 *
 * @checker_framework.manual #methodval-checker MethodVal Checker
 */
@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ MethodVal.class })
@Target({})
public @interface MethodValBottom {
}
