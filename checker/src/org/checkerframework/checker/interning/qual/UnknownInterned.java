package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier for the Interning Checker.
 *
 * <p>
 *
 * This annotation is associated with the {@link InterningChecker}.
 *
 * @see InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownInterned {}
