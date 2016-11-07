package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An identifier, followed by any number of array square brackets.
 *
 * <p>Example: Foobar[][] Example: Baz22
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({
    SourceNameForNonInner.class,
    BinaryNameInUnnamedPackage.class,
    ClassGetSimpleName.class
})
@ImplicitFor(stringPatterns = "^([A-Za-z_][A-Za-z_0-9]*)(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IdentifierOrArray {}
