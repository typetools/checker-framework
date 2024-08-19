package org.checkerframework.checker.confidential.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Denotes a value that can be exposed to end users, or a location that can be accessed by end
 * users. NonConfidential locations can only contain NonConfidential information, not Confidential
 * information.
 *
 * @checker_framework.manual #confidential-checker Confidential Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Confidential.class)
@QualifierForLiterals(LiteralKind.ALL)
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface NonConfidential {}
