package org.checkerframework.checker.units.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * Square kilometer.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Area.class)
public @interface km2 {}
