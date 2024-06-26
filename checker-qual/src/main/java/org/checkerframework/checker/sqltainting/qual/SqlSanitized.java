package org.checkerframework.checker.sqltainting.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Denotes a value to be used in a SQL query that has been sanitized (i.e. non-alphanumeric
 * characters escaped as necessary) and is thus safe for SQL query use.
 *
 * <p>All empty Strings and String literals consisting solely of alphanumeric characters,
 * whitespace, and backslash-escaped sensitive characters (i.e. single/double quotes, backslashes,
 * hyphens, percent signs, underscores) are annotated SQLSanitized automatically.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryValue.class)
@QualifierForLiterals(stringPatterns = "^(\\w|\\s|\\\\'|\\\\\"|\\\\\\\\|\\\\-|\\\\%|\\\\_)*$")
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface SqlSanitized {}
