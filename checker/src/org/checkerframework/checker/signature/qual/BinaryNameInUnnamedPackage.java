package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a string that is both a {@link BinaryName} and an {@link
 * InternalForm}.  The string represents a class that is in the unnamed
 * package (also known as the default package).
 *
 * Example: int
 * Example: int[][]
 * Example: MyClass
 * Example: MyClass[]
 * Example: MyClass$22	
 * Example: MyClass$22[]
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({BinaryName.class, InternalForm.class})
@ImplicitFor(stringPatterns="^[A-Za-z_][A-Za-z_0-9]*(\\$[A-Za-z_0-9]+)*(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BinaryNameInUnnamedPackage {}
