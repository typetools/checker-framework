package org.checkerframework.checker.nullness.compatqual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identical to {@code @MonotonicNonNull}, but can only be written at declaration locations. This
 * annotation can be used in Java 7 code; it has no dependency on Java 8 classes.
 *
 * @see org.checkerframework.checker.nullness.qual.MonotonicNonNull
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface MonotonicNonNullDecl {}
