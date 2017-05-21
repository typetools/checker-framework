package org.checkerframework.common.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Represents a {@link java.lang.reflect.Method Method} or {@link java.lang.reflect.Constructor
 * Constructor} expression whose run-time value is not known at compile time. Also represents
 * non-Method, non-Constructor values.
 *
 * <p>This annotation is the default in the hierarchy.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
@InvisibleQualifier
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@DefaultQualifierInHierarchy
public @interface UnknownMethod {}
