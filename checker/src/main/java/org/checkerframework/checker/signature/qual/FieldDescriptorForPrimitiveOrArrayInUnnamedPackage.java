package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a {@link FieldDescriptor field descriptor} for a primitive or for an array of whose
 * base type is primitive or in the unnamed package.
 *
 * <p>Examples: I, [[J, MyClass, [LMyClass;
 *
 * <p>Field descriptor (JVM type format) is defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.3.2">Java Virtual
 * Machine Specification, section 4.3.2</a>.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({ClassGetName.class, FieldDescriptor.class})
@QualifierForLiterals(stringPatterns = "^([BCDFIJSZ]|\\[+[BCDFIJSZ]|\\[L[A-Za-z_][A-Za-z_0-9]*;)$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface FieldDescriptorForPrimitiveOrArrayInUnnamedPackage {}
