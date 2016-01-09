package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the MethodVal qualifier hierarchy. This is used to
 * make the <tt>null</tt> literal a subtype of all MethodVal annotations.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ MethodVal.class })
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
public @interface MethodValBottom {
}
