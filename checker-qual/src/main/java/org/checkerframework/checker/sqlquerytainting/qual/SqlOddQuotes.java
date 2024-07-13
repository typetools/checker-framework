package org.checkerframework.checker.sqlquerytainting.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * SqlOddQuotes: Used to denote a String that comprises part of a SQL query and contains an odd
 * number of unescaped single quotes – i.e., there must be an odd number of ‘ characters in a
 * SqlOddQuotes String that are not preceded immediately by a \ character. SqlOddQuotes Strings are
 * not syntactical to be passed to query execution methods.
 *
 * <p>Common use cases include: SQL query fragments to be concatenated with user input, such as
 * “SELECT * FROM table WHERE field = ‘”; SQL query fragments containing user input but missing an
 * ending single quote, such as “SELECT * FROM table WHERE field = ‘value”; connecting punctuation,
 * such as “’, “; and any combinations of the above with paired-off single quotes, such as “SELECT *
 * FROM table WHERE field1 = ‘value1’, field2 = ‘value2’, field3 = ‘”.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
@QualifierForLiterals(
    stringPatterns = "^(([^\\\\']|\\\\.)*+')([^\\\\']|'([^\\\\']|\\\\.)*+'|\\\\.)*+\\\\?$")
public @interface SqlOddQuotes {}
