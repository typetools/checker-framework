package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a string that is a {@link BinaryName}, an {@link InternalForm}, and a {@link
 * ClassGetName}. The string represents a class that is in the unnamed package (also known as the
 * default package).
 *
 * <p>Examples:
 *
 * <pre>{@code
 * MyClass
 * MyClass$22
 * }</pre>
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({BinaryName.class, InternalForm.class})
public @interface BinaryNameWithoutPackage {}
