package org.checkerframework.checker.guieffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Annotation for the UI effect.
 *
 * @checker_framework_manual #guieffects-checker GUI Effects Checker
 */
@TypeQualifier
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UI {}
