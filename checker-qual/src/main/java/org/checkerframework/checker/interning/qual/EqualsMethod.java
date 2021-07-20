package org.checkerframework.checker.interning.qual;

import org.checkerframework.framework.qual.InheritedAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method declaration annotation that indicates a method has a specification like {@code equals()}.
 * The Interning Checker permits use of {@code this == arg} within the body. Can also be applied to
 * a static two-argument method, in which case {@code arg1 == arg2} is permitted within the body.
 *
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@InheritedAnnotation
public @interface EqualsMethod {}
