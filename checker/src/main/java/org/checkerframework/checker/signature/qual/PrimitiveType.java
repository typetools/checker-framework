package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A primitive type. One of: boolean, byte, char, double, float, int, long, short.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({IdentifierOrPrimitiveType.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@QualifierForLiterals(
        stringPatterns =
                /* Do not edit; see SignatureRegexes.java */ "^boolean|byte|char|double|float|int|long|short$")
public @interface PrimitiveType {}
