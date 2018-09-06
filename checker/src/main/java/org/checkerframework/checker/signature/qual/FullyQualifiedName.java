package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a fully-qualified name as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se10/html/jls-6.html#jls-6.7">Java Language
 * Specification, section 6.7</a>.
 *
 * <p>For example, in
 *
 * <pre>
 *  package org.checkerframework.checker.signature;
 *  public class SignatureChecker {
 *    private class Inner {}
 *  }
 * </pre>
 *
 * the fully-qualified names for the two types are
 * org.checkerframework.checker.signature.SignatureChecker and
 * org.checkerframework.checker.signature.SignatureChecker.Inner.
 *
 * <p>Fully-qualified names and {@linkplain BinaryName binary names} are the same for top-level
 * classes and only differ by a '.' vs. '$' for inner classes.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf(SignatureUnknown.class)
@ImplicitFor(stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FullyQualifiedName {}
