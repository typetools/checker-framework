package org.checkerframework.common.reflection.qual;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Represents the bottom of the MethodVal qualifier hierarchy. This is used to
 * make the <code>null</code> literal a subtype of all MethodVal annotations.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
@InvisibleQualifier
@ImplicitFor(literals = { LiteralKind.NULL }, typeNames = { java.lang.Void.class })
@SubtypeOf({ MethodVal.class })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface MethodValBottom {
}
