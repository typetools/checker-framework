package org.checkerframework.common.value.qual;

import com.sun.source.tree.Tree;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;

/**
 * Represents the bottom of the Constant Value qualifier hierarchy.  It means that
 * the value always has the value null or that the expression is dead code.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@InvisibleQualifier
@ImplicitFor(trees = { Tree.Kind.NULL_LITERAL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ ArrayLen.class, BoolVal.class, DoubleVal.class,
        IntVal.class, StringVal.class })
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
public @interface BottomVal {
}
