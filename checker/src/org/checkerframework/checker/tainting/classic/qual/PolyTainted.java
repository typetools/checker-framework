package org.checkerframework.checker.tainting.classic.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

/**
 * A polymorphic qualifier for the Tainting type system.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyTainted {}
