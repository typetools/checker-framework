package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The syntax for binary names that appears in a class file, as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.2">JVM Specification,
 * section 4.2</a>, but only for a non-array type. A {@linkplain BinaryName binary name} is
 * conceptually the name for the class or interface in a compiled binary, but the actual
 * representation of that name in its class file is slightly different.
 *
 * <p>Internal form is the same as the binary name, but with periods ({@code .}) replaced by forward
 * slashes ({@code /}).
 *
 * <p>Programmers more often use the binary name, leaving the internal form as a JVM implementation
 * detail.
 *
 * @see InternalForm
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({InternalForm.class})
@ImplicitFor(
    stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(/[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_0-9]+)*$"
)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface InternalFormForNonArray {}
