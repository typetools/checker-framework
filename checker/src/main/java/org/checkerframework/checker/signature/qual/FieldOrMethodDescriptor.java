package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a field descriptor or a method descriptor (JVM type format) as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2">Java Virtual
 * Machine Specification, section 4.3.2</a>.
 *
 * <p>For example, in
 *
 * <pre>
 *  package org.checkerframework.checker.signature;
 *  public class SignatureChecker {
 *    private class Inner {}
 *
 *    void func(int a, String b) {
 *      // ...
 *    }
 *  }
 * </pre>
 *
 * the field descriptors for the two types are
 * Lorg/checkerframework/checker/signature/SignatureChecker; and
 * Lorg/checkerframework/checker/signature/SignatureChecker$Inner; .
 *
 * <p>and the method descriptor for {@code func} is: (ILjava/lang/String;)V
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf(SignatureUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FieldOrMethodDescriptor {}
