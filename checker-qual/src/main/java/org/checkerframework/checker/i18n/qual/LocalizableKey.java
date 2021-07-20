package org.checkerframework.checker.i18n.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the {@code String} is a key into a property file or resource bundle containing
 * Localized Strings.
 *
 * @checker_framework.manual #i18n-checker Internationalization Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownLocalizableKey.class)
public @interface LocalizableKey {}
