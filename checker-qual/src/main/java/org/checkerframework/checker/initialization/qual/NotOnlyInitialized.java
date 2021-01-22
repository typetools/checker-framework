package org.checkerframework.checker.initialization.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration annotation for fields that indicates that a client might observe the field storing
 * values that are {@link org.checkerframework.checker.initialization.qual.Initialized}, {@link
 * org.checkerframework.checker.initialization.qual.UnderInitialization}, or {@link
 * org.checkerframework.checker.initialization.qual.UnknownInitialization}, regardless of the
 * initialization type annotation on the field's type. This is necessary to allow circular
 * initialization as supported by freedom-before-commitment.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotOnlyInitialized {}
