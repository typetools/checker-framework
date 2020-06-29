package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The type representation used by the {@link Class#getName()}, {@link Class#forName(String)}, and
 * {@link Class#forName(String, boolean, ClassLoader)} methods. This format is:
 *
 * <ul>
 *   <li>for any non-array type, the {@link BinaryName binary name}
 *   <li>for any array type, a format like the {@link FieldDescriptor field descriptor}, but using
 *       '.' where the field descriptor uses '/'
 * </ul>
 *
 * <p>Examples include
 *
 * <pre>
 *   java.lang.String
 *   [Ljava.lang.Object;
 *   int
 *   [[[I
 * </pre>
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SignatureUnknown.class)
public @interface ClassGetName {}
