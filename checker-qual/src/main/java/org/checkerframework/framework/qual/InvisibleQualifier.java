package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation indicating that an annotation is a type qualifier that should not be shown in
 * output. {@code @InvisibleQualifier} should only be written on a qualifier that is also
 * meta-annotated with {@code @}{@link DefaultQualifierInHierarchy}.
 *
 * <p>By default, the Checker Framework's error messages show every annotation, including inferred
 * and default ones (which far outnumber the ones explicitly written by the programmer). Being
 * explicit helps users understand the effective annotations, which the Checker Framework operates
 * upon. However, the output can be verbose, and it can show annotations that a user should not
 * write. For example, a Format String Checker warning message might contain
 * "{@code @UnknownCompilerMessageKey Map<@CompilerMessageKey String, @UnknownCompilerMessageKey
 * String>}".
 *
 * <p>When an annotation is meta-annotated with {@code @InvisibleQualifier}, then the Checker
 * Framework does not print the given qualifier when printing types. For the above example, the
 * Format String Checker prints "{@code Map<@CompilerMessageKey String, String>}".
 *
 * <p>If a user runs the Checker Framework with command-line argument {@code -AprintAllQualifiers},
 * then invisible qualifiers are output.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InvisibleQualifier {}
