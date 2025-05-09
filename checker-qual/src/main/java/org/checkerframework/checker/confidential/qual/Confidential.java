package org.checkerframework.checker.confidential.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a value that will not be exposed to end users or a sink that will not be able to be
 * accessed by end users.
 *
 * <p>Typically, a Confidential value will contain sensitive, private, or otherwise
 * privileged-access information, such as passwords, PII, and private keys.
 *
 * @see NonConfidential
 * @see org.checkerframework.checker.confidential.ConfidentialChecker
 * @checker_framework.manual #confidential-checker Confidential Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownConfidential.class)
public @interface Confidential {}
