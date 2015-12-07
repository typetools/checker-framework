package org.checkerframework.checker.initialization.qual;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration annotation for fields that indicates that a client might
 * observe the field storing values that are {@link Initialized},
 * {@link UnderInitialization}, or {@link UnknownInitialization}, regardless
 * of the initialization type annotation on the field's type. This is
 * necessary to allow circular initialization as supported by
 * freedom-before-commitment.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @author Stefan Heule
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotOnlyInitialized {
}
