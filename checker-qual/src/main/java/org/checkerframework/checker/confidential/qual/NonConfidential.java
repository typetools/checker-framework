package org.checkerframework.checker.confidential.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Denotes a value that may be exposed to end users, or a location that may be accessed by end
 * users. NonConfidential locations will never contain sensitive, private, or otherwise
 * privileged-access information.
 *
 * @checker_framework.manual #confidential-checker Confidential Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownConfidential.class)
@QualifierForLiterals({LiteralKind.STRING, LiteralKind.PRIMITIVE})
@DefaultQualifierInHierarchy
@DefaultFor(value = {TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.UPPER_BOUND})
public @interface NonConfidential {}
