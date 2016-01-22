package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Dominator.class)
@DefaultQualifierInHierarchy
@DefaultFor({DefaultLocation.IMPLICIT_UPPER_BOUNDS, DefaultLocation.EXCEPTION_PARAMETER})
public @interface Modifier {}
