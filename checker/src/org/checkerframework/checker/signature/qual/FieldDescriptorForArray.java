package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a {@link FieldDescriptor field descriptor} (JVM type format) as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2">Java Virtual
 * Machine Specification, section 4.3.2</a>, but <b>not</b> for all array types: only for an array
 * type whose base type is either a primitive or in the unnamed package. Also non-array primitives.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({ClassGetName.class, FieldDescriptor.class})
@ImplicitFor(stringPatterns = "^([BCDFIJSZ]|\\[+[BCDFIJSZ]|\\[L[A-Za-z_][A-Za-z_0-9]*;)$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FieldDescriptorForArray {}
