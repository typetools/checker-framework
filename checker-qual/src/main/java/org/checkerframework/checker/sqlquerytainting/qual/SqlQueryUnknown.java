package org.checkerframework.checker.sqlquerytainting.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/** Represents the top of the SQL query tainting qualifier hierarchy. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface SqlQueryUnknown {}
