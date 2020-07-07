package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * Method declaration annotation that indicates a method has a specification like {@code
 * compareTo()} or {@code compare()}. The Interning Checker permits use of {@code if (this == arg) {
 * return 0; }} or {@code if (arg1 == arg2) { return 0; }} within the body.
 *
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@InheritedAnnotation
public @interface CompareToMethod {}
