package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An identifier.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({
    DotSeparatedIdentifiers.class,
    BinaryNameInUnnamedPackage.class,
    IdentifierOrArray.class
})
@QualifierForLiterals(stringPatterns = "^([A-Za-z_][A-Za-z_0-9]*)$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Identifier {}
