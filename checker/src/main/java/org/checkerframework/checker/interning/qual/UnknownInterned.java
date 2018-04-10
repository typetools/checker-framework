package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier for the Interning Checker. It indicates lack of knowledge about whether values
 * are interned or not. It is not written by programmers, but is used internally by the type system.
 *
 * <p>This annotation is associated with the {@link
 * org.checkerframework.checker.interning.InterningChecker}.
 *
 * @see org.checkerframework.checker.interning.InterningChecker
 * @checker_framework.manual #interning-checker Interning Checker
 */
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UnknownInterned {}
