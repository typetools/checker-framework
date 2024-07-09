package org.checkerframework.checker.sqlquerytainting;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a String that comprises part of a SQL query - commonly, it may be a SQL command fragment,
 * a command requiring user input concatenation, or a command with user input requiring end quote
 * concatenation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
public @interface SqlQuerySnippet {}
