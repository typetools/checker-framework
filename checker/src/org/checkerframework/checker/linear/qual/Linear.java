package org.checkerframework.checker.linear.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * @checker_framework.manual #linear-checker Linear Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
@SubtypeOf(Normal.class)
public @interface Linear {}
