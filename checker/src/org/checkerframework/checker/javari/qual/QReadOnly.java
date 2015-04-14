package org.checkerframework.checker.javari.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.javari.JavariChecker;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Indicates that the annotated type behaves as the most restrictive of
 * {@link ReadOnly} and {@link Mutable}: only {@link Mutable} can be assigned
 * to it, and it can only be assigned to {@link ReadOnly}.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @see JavariChecker
 * @checker_framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(ReadOnly.class)
public @interface QReadOnly {}
