package org.checkerframework.checker.sqlquerytainting.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Used to denote a String that comprises part of a SQL query and
 * contains an even number of unescaped single quotes – i.e., there
 * must be an even number of ‘ characters in a SqlEvenQuotes String
 * that are not preceded immediately by a / character. SQLEvenQuotes
 * Strings are safe to be passed to query execution methods.
 *
 * Common use cases include: complete SQL queries, such as “SELECT *
 * FROM table WHERE field = ‘value’”.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
@QualifierForLiterals(stringPatterns = "^(([^\\\\\\']|\\\\.)*\\'){2}([^\\\\\\']|(\\'([^\\\\\\']|\\\\.)*\\')|\\\\.)*\\\\?$")
public @interface SqlEvenQuotes {}
