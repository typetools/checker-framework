package org.checkerframework.checker.i18n.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.propkey.qual.PropertyKey;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that the {@code String} is a key into a property file
 * or resource bundle containing Localized Strings.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@TypeQualifier
@SubtypeOf(PropertyKey.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LocalizableKey {}
