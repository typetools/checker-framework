package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier
public @interface WildCard {}
