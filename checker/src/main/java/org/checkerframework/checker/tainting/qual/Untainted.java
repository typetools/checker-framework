package org.checkerframework.checker.tainting.qual;

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
 * Denotes a reference that is untainted, i.e. can be trusted.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@SubtypeOf(Tainted.class)
@QualifierForLiterals(LiteralKind.STRING)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface Untainted {}
