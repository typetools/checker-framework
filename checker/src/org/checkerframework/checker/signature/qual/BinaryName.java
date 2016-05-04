package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a binary name as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">Java
 * Language Specification, section 13.1</a>.
 * <p>
 *
 * For example, in
 * <pre>
 *  package org.checkerframework.checker.signature;
 *  public class SignatureChecker {
 *    private class Inner {}
 *  }
 * </pre>
 * the binary names for the two types are org.checkerframework.checker.signature.SignatureChecker
 * and org.checkerframework.checker.signature.SignatureChecker$Inner.
 * <p>
 *
 * Binary names and {@linkplain FullyQualifiedName fully qualified names} are the
 * same for top-level classes and only differ by '$' vs. '.' for inner classes.
 * <p>
 *
 * The binary name should not be confused with the {@linkplain InternalForm
 * internal form}, which is a variant of the binary name that actually
 * appears in the class file.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf(SignatureUnknown.class)
@ImplicitFor(stringPatterns="^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_0-9]+)*(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface BinaryName {}
