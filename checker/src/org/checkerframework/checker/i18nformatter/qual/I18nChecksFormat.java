package org.checkerframework.checker.i18nformatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Attach this annotation to a method with the following properties:
 * <ul>
 *   <li>The first parameter is a format string.</li>
 *   <li>The second parameter is a vararg that takes conversion categories.</li>
 *   <li>The method returns true if the format string is compatible with the conversion categories.</li>
 * </ul>
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization
 *                           Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface I18nChecksFormat {}
