package org.checkerframework.common.util.count.report.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation intended solely for representing an unqualified type in the qualifier hierarchy for
 * the Report Checker. This is the only type qualifier in the type hierarchy.
 */
@Documented
@Retention(RetentionPolicy.SOURCE) // do not store in class file
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
@InvisibleQualifier
public @interface ReportUnqualified {}
