package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attach this annotation to a method with the following properties:
 *
 * <ul>
 *   <li>The first parameter is a format string.
 *   <li>The second parameter is a vararg that takes conversion categories.
 *   <li>The method throws an exception if the format string's format specifiers do not match the
 *       passed conversion categories.
 *   <li>On success, the method returns the passed format string unmodified.
 * </ul>
 *
 * An example is {@code FormatUtil#asFormat()}.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnsFormat {}
