package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating ownership should not be transferred to the parameter, field, or return
 * type, for the purposes of MustCall checking.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NotOwning {}
