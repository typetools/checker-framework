package org.checkerframework.checker.sqlquotes.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents the top of the SQL Quotes qualifier hierarchy. Used to denote a String of which the
 * quoting is unknown. Using a SqlQuotesUnknown Strings within a SQL query may be, or lead to, a SQL
 * injection vulnerability.
 *
 * <p>Common use cases include unsanitized user input.
 *
 * @checker_framework.manual #sql-quotes-checker SQL Quotes Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface SqlQuotesUnknown {}
