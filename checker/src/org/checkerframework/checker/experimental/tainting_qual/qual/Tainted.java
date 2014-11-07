package org.checkerframework.checker.experimental.tainting_qual.qual;

import org.checkerframework.checker.experimental.tainting_qual.TaintingChecker;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier of the tainting type system.
 * This annotation is associated with the {@link TaintingChecker}.
 *
 * @see Untainted
 * @see TaintingChecker
 * @checker_framework_manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface Tainted {}
