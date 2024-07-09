package org.checkerframework.checker.sqlquerytainting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a value to be used in a SQL query originating from user input that has been sanitized
 * (i.e. non-alphanumeric characters escaped as necessary) and is thus safe to be embedded as a
 * value in a SQL query.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlQueryUnknown.class)
@QualifierForLiterals(stringPatterns = "^(\\w|\\s|(\\\\[-'\"\\\\%_])*)*$")
public @interface SqlSanitizedUser {}
