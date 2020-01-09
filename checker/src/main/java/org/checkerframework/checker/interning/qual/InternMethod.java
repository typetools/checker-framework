package org.checkerframework.checker.interning.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method declaration annotation used to indicate that this method may be invoked on an uninterned
 * object and that it returns an interned object.
 *
 * @checker_framework.manual #interning-checker Interning Checker
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface InternMethod {}
