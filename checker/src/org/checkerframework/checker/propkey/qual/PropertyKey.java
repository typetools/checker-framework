package org.checkerframework.checker.propkey.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the {@code String} type can be used as key in a
 * property file or resource bundle.
 *
 * @checker_framework.manual #propkey-checker Property File Checker
 */
@SubtypeOf(UnknownPropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PropertyKey {}
