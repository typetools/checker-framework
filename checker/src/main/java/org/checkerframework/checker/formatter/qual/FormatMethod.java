package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this annotation is attached to a {@link java.util.Formatter#format(String, Object...)
 * Formatter.format}-like method, then the first, String parameter is treated as a format string for
 * the remaining arguments. The Format String Checker ensures that the arguments passed as varargs
 * are compatible with the format string argument, and also permits them to be passed to {@link
 * java.util.Formatter#format(String, Object...) Formatter.format}-like methods within the body.
 *
 * <p>The initial String parameter may optionally be preceded by a Locale parameter.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface FormatMethod {}
