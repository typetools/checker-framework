package org.checkerframework.checker.linear.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * @checker_framework.manual #linear-checker Linear Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Unusable.class)
@DefaultQualifierInHierarchy
public @interface Normal {}
