package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A sequence of dot-separated identifiers, followed by any number of array square brackets.
 * Represents a fully-qualified name as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.7">Java Language
 * Specification, section 6.7</a>.
 *
 * <p>Examples:
 *
 * <pre>{@code
 * int
 * MyClass
 * java.lang.Integer
 * int[][]
 * MyClass[]
 * java.lang.Integer[][][]
 * }</pre>
 *
 * <p>in
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
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FqBinaryName.class)
public @interface FullyQualifiedName {}
