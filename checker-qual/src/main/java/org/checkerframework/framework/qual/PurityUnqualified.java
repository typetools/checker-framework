package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation intended solely for representing an unqualified type in the qualifier hierarchy for
 * the Purity Checker.
 *
 * @checker_framework.manual #purity-checker Purity Checker
 */
@Documented
@Retention(RetentionPolicy.SOURCE) // do not store in .class file
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
@InvisibleQualifier
public @interface PurityUnqualified {}
