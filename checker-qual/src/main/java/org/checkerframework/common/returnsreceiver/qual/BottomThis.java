package org.checkerframework.common.returnsreceiver.qual;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The bottom type for the Returns Receiver Checker's type system. Programmers should rarely write
 * this type.
 *
 * @checker_framework.manual #returns-receiver-checker Returns Receiver Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@SubtypeOf({UnknownThis.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
public @interface BottomThis {}
