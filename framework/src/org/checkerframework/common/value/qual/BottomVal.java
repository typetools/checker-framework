package org.checkerframework.common.value.qual;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Represents the bottom of the Constant Value qualifier hierarchy.  It means that
 * the value always has the value null or that the expression is dead code.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@InvisibleQualifier
@ImplicitFor(literals = { LiteralKind.NULL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ ArrayLen.class, BoolVal.class, DoubleVal.class,
        IntVal.class, StringVal.class })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface BottomVal {
}
