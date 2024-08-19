package org.checkerframework.checker.confidential.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Denotes a value that should not be exposed to end users (i.e., PII, passwords, and private keys)
 * or a location (i.e., a file or database) that should not be able to be accessed by end users.
 * Confidential locations can contain Confidential or NonConfidential information.
 *
 * @see NonConfidential
 * @see org.checkerframework.checker.confidential.ConfidentialChecker
 * @checker_framework.manual #confidential-checker Confidential Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Confidential {}
