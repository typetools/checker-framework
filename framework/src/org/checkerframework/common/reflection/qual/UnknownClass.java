package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents a Class object whose run-time value is not known at compile time.
 * Also represents non-Class values.
 * <p>
 *
 * This annotation is the default in the hierarchy and may not be written in
 * source code.
 *
 * @checker_framework.manual #methodval-and-classval-checkers ClassVal Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@DefaultQualifierInHierarchy
public @interface UnknownClass {
}
