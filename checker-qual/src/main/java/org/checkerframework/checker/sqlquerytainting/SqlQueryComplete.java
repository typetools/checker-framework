package org.checkerframework.checker.sqlquerytainting;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a String that represents a complete, safe SQL query to be executed. Safety from SQL
 * injection vulnerabilities is guaranteed as all components of @SqlQueryComplete Strings either
 * originate entirely within the author's code, or originate from user input and have been
 * sanitized.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
public @interface SqlQueryComplete {}
