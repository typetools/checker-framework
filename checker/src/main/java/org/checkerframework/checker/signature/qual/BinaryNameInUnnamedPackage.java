package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a string that is a {@link BinaryName}, an {@link InternalForm}, and a {@link
 * ClassGetName}. The string represents a class that is in the unnamed package (also known as the
 * default package).
 *
 * <p>Examples: {@code MyClass}, {@code MyClass$22}.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({BinaryName.class, InternalForm.class})
@ImplicitFor(stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\$[A-Za-z_0-9]+)*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BinaryNameInUnnamedPackage {}
