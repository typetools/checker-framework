package org.checkerframework.checker.sqlquotes.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a String that contains an odd number of unescaped single quotes -- i.e., there must be an
 * odd number of ' characters in a SqlOddQuotes String that are not preceded immediately by another
 * ' character. (Thus, all SqlOddQuotes Strings ultimately contain an odd number of single quotes,
 * escaped or otherwise.) SqlOddQuotes Strings are not syntactical to be passed to query execution
 * methods.
 *
 * <p>Common use cases include: SQL query fragments to be concatenated with user input, such as
 * "SELECT * FROM table WHERE field = '"; SQL query fragments containing user input but missing an
 * ending single quote, such as "SELECT * FROM table WHERE field = 'value"; connecting punctuation,
 * such as "', "; and any combinations of the above with paired-off single quotes, such as "SELECT *
 * FROM table WHERE field1 = 'value1', field2 = 'value2', field3 = '".
 *
 * @checker_framework.manual #sql-quotes-checker SQL Quotes Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQuotesUnknown.class)
public @interface SqlOddQuotes {}
