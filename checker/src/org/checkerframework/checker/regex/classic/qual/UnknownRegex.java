package org.checkerframework.checker.regex.classic.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;

/**
 * Represents the top of the Regex qualifier hierarchy.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@InvisibleQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE})
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
public @interface UnknownRegex {}
