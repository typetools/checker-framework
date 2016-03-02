package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the {@code String} type has an unknown
 * property key property.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownPropertyKey {}
