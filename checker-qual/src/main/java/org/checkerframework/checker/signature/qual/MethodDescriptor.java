package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a method descriptor (JVM representation of method signature) as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.3.3">Java Virtual
 * Machine Specification, section 4.3.3</a>.
 *
 * <p>Example:
 *
 * <pre>
 *  package edu.cs.washington;
 *  public class BinaryName {
 *    private class Inner {
 *      public void method(Object obj, int i) {}
 *    }
 *  }
 * </pre>
 *
 * In this example method descriptor for method 'method': (Ljava/lang/Object;I)Z
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SignatureUnknown.class)
public @interface MethodDescriptor {}
