package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Canonical names have the same syntactic form as {@link FullyQualifiedName fully-qualified name}s.
 * Every canonical name is a fully-qualified name, but not every fully-qualified name is a canonical
 * name.
 *
 * <p><a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.7">JLS section
 * 6.7</a> gives the following example:
 *
 * <blockquote>
 *
 * The difference between a fully qualified name and a canonical name can be seen in code such as:
 *
 * <pre>{@code
 * package p;
 * class O1 { class I {} }
 * class O2 extends O1 {}
 * }</pre>
 *
 * Both {@code p.O1.I} and {@code p.O2.I} are fully qualified names that denote the member class
 * {@code I}, but only {@code p.O1.I} is its canonical name.
 *
 * </blockquote>
 *
 * Given a character sequence that is a fully-qualified name, there is no way to know whether or not
 * it is a canonical name, without examining the program it refers to. Type-checking determines that
 * a string is a {@code CanonicalName} based on provenance (what method produced the string), rather
 * than the contents of the string.
 *
 * @see FullyQualifiedName
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({
  FullyQualifiedName.class,
  CanonicalNameOrEmpty.class,
  CanonicalNameOrPrimitiveType.class
})
public @interface CanonicalName {}
