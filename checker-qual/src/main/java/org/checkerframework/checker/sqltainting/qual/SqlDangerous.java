package org.checkerframework.checker.sqltainting.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a possibly-dangerous value: at run time, the value might be safe for SQL query use or
 * might be dangerous.
 *
 * @see SqlSafe
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy // unannotated values are marked SqlTainted
public @interface SqlDangerous {}
