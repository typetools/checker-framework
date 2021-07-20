package org.checkerframework.checker.signature.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An identifier.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({
    DotSeparatedIdentifiers.class,
    BinaryNameWithoutPackage.class,
    IdentifierOrPrimitiveType.class
})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Identifier {}
