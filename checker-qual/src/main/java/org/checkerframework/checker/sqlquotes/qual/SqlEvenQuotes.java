package org.checkerframework.checker.sqlquotes.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a String that contains either zero or an even number of unescaped single quotes &mdash;
 * i.e., there must be either zero or an even number of {@code '} characters in a SqlEvenQuotes
 * String that are not preceded immediately by another {@code '} character. (Thus, all SqlEvenQuotes
 * Strings ultimately contain an even number of {@code '} characters.) A SqlEvenQuotes String is
 * syntactical to be passed to query execution methods and is guaranteed not to affect whether
 * subsequent concatenations are interpreted as SQL command code or as SQL query values.
 *
 * <p>Common use cases include: SQL query fragments, such as "SELECT * FROM"; properly sanitized
 * user input; and complete SQL queries, such as "SELECT * FROM table WHERE field = 'value'".
 *
 * @checker_framework.manual #sql-quotes-checker SQL Quotes Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQuotesUnknown.class)
public @interface SqlEvenQuotes {}
