package org.checkerframework.checker.sqlquotes.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
     * Used to denote a String that contains either zero or an even number of unescaped single quotes –
 * i.e., there must be either zero or an even number of ‘ characters in a SqlEvenQuotes String that
 * are not preceded immediately by a SQL \ character. (Thus, written in Java, every occurrence of
 * the single quote in a SqlEvenQuotes String must be preceded by a double backslash.) SqlEvenQuotes
 * Strings are syntactical to be passed to query execution methods and are guaranteed to have no
 * impact on whether subsequent concatenations are interpreted as SQL command code or SQL query
 * values.
 *
 * <p>Common use cases include: SQL query fragments, such as “SELECT * FROM”; properly sanitized
 * user input; and complete SQL queries, such as “SELECT * FROM table WHERE field = ‘value’”.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQuotesUnknown.class)
@QualifierForLiterals(stringPatterns = "^((([^\\\\']|\\\\.)*+'){2})*+([^\\\\']|\\\\.)*+\\\\?$")
public @interface SqlEvenQuotes {}
