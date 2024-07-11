package org.checkerframework.checker.sqlquerytainting.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Used to denote a String that comprises part of a SQL query and
 * contains exactly zero unescaped single quotes – i.e., all occurrences
 * of the ‘ character in such a String are preceded immediately by a /
 * character. SQLQuoteless Strings are safe to be passed to query execution
 * methods.
 *
 * Common use cases include SQL query fragments such as “SELECT * FROM”
 * and properly sanitized user input.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
@QualifierForLiterals(stringPatterns = "^([^\\']|(\\\\'))*$")
public @interface SqlQuoteless {}
