package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a binary name as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-13.html#jls-13.1">Java Language
 * Specification, section 13.1</a>.
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
 * the binary names for the two types are org.checkerframework.checker.signature.SignatureChecker
 * and org.checkerframework.checker.signature.SignatureChecker$Inner.
 *
 * <p>Binary names and {@linkplain InternalForm internal form} only differ by the use of '.' vs. '/'
 * as package separator.
 *
 * <p>The binary name should not be confused with the {@linkplain InternalForm internal form}, which
 * is a variant of the binary name that actually appears in the class file.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({BinaryNameOrPrimitiveType.class})
public @interface BinaryName {}
