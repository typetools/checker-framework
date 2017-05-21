package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a {@link BinaryName binary name} as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">Java Language
 * Specification, section 13.1</a>, but only for a non-array type in the unnamed package (also known
 * as the default package).
 *
 * <p>Example: Foo$Inner Example: Foo$22 Example: Foo
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({BinaryNameForNonArray.class, BinaryNameInUnnamedPackage.class})
@ImplicitFor(stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\$[A-Za-z_0-9]+)*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BinaryNameForNonArrayInUnnamedPackage {}
