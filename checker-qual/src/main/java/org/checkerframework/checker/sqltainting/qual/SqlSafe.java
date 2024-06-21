package org.checkerframework.checker.sqltainting.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Denotes a reference that is SQL-safe, i.e., originates from a trusted source or has been
 * sanitized.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SqlDangerous.class)
@QualifierForLiterals(LiteralKind.STRING)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface SqlSafe {}
