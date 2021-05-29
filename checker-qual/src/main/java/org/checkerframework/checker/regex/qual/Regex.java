package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * If a type is annotated as {@code @Regex(n)}, then the run-time value is a regular expression with
 * <em>n</em> capturing groups.
 *
 * <p>For example, if an expression's type is <em>@Regex(2) String</em>, then at run time its value
 * will be a legal regular expression with at least two capturing groups. The type states that
 * possible run-time values include {@code "(a*)(b*)"}, {@code "a(b?)c(d?)e"}, and {@code
 * "(.)(.)(.)"}, but not {@code "hello"} nor {@code "(good)bye"} nor {@code "(a*)(b*)("}.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownRegex.class)
public @interface Regex {
  /** The number of groups in the regular expression. Defaults to 0. */
  int value() default 0;
}
