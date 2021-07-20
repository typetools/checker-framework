package org.checkerframework.checker.i18nformatter.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The bottom type in the Internationalization Format String type system. Programmers should rarely
 * write this type.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 * @checker_framework.manual #bottom-type the bottom type
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@SubtypeOf({I18nFormat.class, I18nInvalidFormat.class, I18nFormatFor.class})
@DefaultFor(value = {TypeUseLocation.LOWER_BOUND})
public @interface I18nFormatBottom {}
