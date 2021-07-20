package org.checkerframework.common.reflection.qual;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a {@link java.lang.reflect.Method Method} or {@link java.lang.reflect.Constructor
 * Constructor} expression whose run-time value is not known at compile time. Also represents
 * non-Method, non-Constructor values.
 *
 * <p>This annotation is the default in the hierarchy.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface UnknownMethod {}
