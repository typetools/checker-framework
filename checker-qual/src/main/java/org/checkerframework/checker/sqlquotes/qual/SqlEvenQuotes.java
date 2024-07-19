package org.checkerframework.checker.sqlquotes.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * SqlEvenQuotes: Used to denote a String that comprises part of a SQL query and contains either
 * zero or an even number of unescaped single quotes – i.e., there must be either zero or an even
 * number of ‘ characters in a SqlEvenQuotes String that are not preceded immediately by a \
 * character. SqlEvenQuotes Strings are syntactical and safe to be passed to query execution
 * methods.
 *
 * <p>Common use cases include: SQL query fragments, such as “SELECT * FROM”; properly sanitized
 * user input; and complete SQL queries, such as “SELECT * FROM table WHERE field = ‘value’”.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQuotesUnknown.class)
@QualifierForLiterals(
    stringPatterns =
        "^"
            // any number of paired quotes
            + "((([^\\\\']|\\\\.)*+'){2})*+"
            // no quotes
            + "([^\\\\']|\\\\.)*+"
            // possible final backslash
            + "\\\\?"
            + "$")
public @interface SqlEvenQuotes {}
