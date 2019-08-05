package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a field descriptor (JVM type format) for a primitive as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.3.2">Java Virtual
 * Machine Specification, section 4.3.2</a>.
 *
 * <p>Must be one of B, C, D, F, I, J, S, Z.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({FieldDescriptorForPrimitiveOrArrayInUnnamedPackage.class, Identifier.class})
@QualifierForLiterals(stringPatterns = "^[BCDFIJSZ]$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FieldDescriptorForPrimitive {}
