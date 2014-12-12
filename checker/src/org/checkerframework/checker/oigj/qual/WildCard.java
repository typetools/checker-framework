package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@PolymorphicQualifier
public @interface WildCard {}
